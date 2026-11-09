package pl.duoduo.app

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class LearningEngineTest {
    private fun word(answer: String = "water", policy: AnswerPolicy = AnswerPolicy()) = Exercise("word", ExerciseType.word_input, "Wpisz", answers = listOf(listOf(answer)), answerPolicy = policy)
    private fun grade(answer: String, exercise: Exercise = word()) = LearningEngine.grade(exercise, Response(texts = listOf(answer)))

    @Test fun exactAndWhitespaceAreAccepted() { assertEquals(Verdict.correct, grade(" WATER ").verdict) }
    @Test fun oneReplacementIsMarkedAsTypo() { assertEquals(Verdict.typo, grade("wator").verdict) }
    @Test fun adjacentTranspositionIsAccepted() { assertEquals(Verdict.typo, grade("waetr").verdict) }
    @Test fun insertionAndDeletionAreAccepted() { assertEquals(Verdict.typo, grade("waterr").verdict); assertEquals(Verdict.typo, grade("watr").verdict) }
    @Test fun shortWordsMustBeExact() { assertEquals(Verdict.incorrect, grade("cot", word("cat")).verdict) }
    @Test fun multipleErrorsAreRejected() { assertEquals(Verdict.incorrect, grade("wotor").verdict) }
    @Test fun emptyInputIsRejected() { assertEquals(Verdict.incorrect, grade("").verdict) }
    @Test fun unicodeCanonicalEquivalenceIsAccepted() { assertEquals(Verdict.correct, grade("cafe\u0301", word("café")).verdict) }
    @Test fun codePolicyPreservesCaseAndRejectsTypos() {
        val code = word("print", AnswerPolicy(allowTypos = false, caseSensitive = true))
        assertEquals(Verdict.incorrect, grade("Print", code).verdict)
        assertEquals(Verdict.incorrect, grade("pritn", code).verdict)
    }
    @Test fun feedbackShowsClosestAcceptedVariant() {
        val e = word().copy(answers = listOf(listOf("colour", "color")))
        val result = grade("colro", e)
        assertEquals(Verdict.typo, result.verdict)
        assertEquals("color", result.expected)
        assertEquals("colro", result.submitted)
    }
    @Test fun allGapsMustBeCorrect() {
        val e = word().copy(type = ExerciseType.sentence_input, answers = listOf(listOf("has"), listOf("red")))
        assertEquals(Verdict.incorrect, LearningEngine.grade(e, Response(texts = listOf("has", "blue"))).verdict)
        assertEquals(Verdict.incorrect, LearningEngine.grade(e, Response(texts = listOf("has"))).verdict)
    }
    @Test fun multiChoiceUsesExactSet() {
        val e = Exercise("multi", ExerciseType.choice, "Wybierz", options = listOf(Choice("a", "A"), Choice("b", "B"), Choice("c", "C")), correctOptionIds = listOf("a", "b"), multiple = true)
        assertEquals(Verdict.correct, LearningEngine.grade(e, Response(selected = listOf("b", "a"))).verdict)
        assertEquals(Verdict.incorrect, LearningEngine.grade(e, Response(selected = listOf("a"))).verdict)
        assertEquals(Verdict.incorrect, LearningEngine.grade(e, Response(selected = listOf("a", "b", "c"))).verdict)
        assertEquals(Verdict.incorrect, LearningEngine.grade(e, Response(selected = listOf("a", "a", "b"))).verdict)
    }
    @Test fun tileOrderMatters() {
        val e = Exercise("order", ExerciseType.sentence_order, "Ułóż", options = listOf(Choice("a", "I"), Choice("b", "am")), correctOptionIds = listOf("a", "b"))
        assertEquals(Verdict.correct, LearningEngine.grade(e, Response(selected = listOf("a", "b"))).verdict)
        assertEquals(Verdict.incorrect, LearningEngine.grade(e, Response(selected = listOf("b", "a"))).verdict)
    }
    @Test fun repeatedWordsHaveIndependentIdentities() {
        val e = Exercise("select", ExerciseType.sentence_select, "Wybierz", options = listOf(Choice("a", "the"), Choice("b", "the")), correctOptionIds = listOf("b"))
        assertEquals(Verdict.incorrect, LearningEngine.grade(e, Response(selected = listOf("a"))).verdict)
        assertEquals(Verdict.correct, LearningEngine.grade(e, Response(selected = listOf("b"))).verdict)
    }
    @Test fun mistakesReturnAfterTwoExercises() {
        val session = Session("c@1", "L", listOf("a", "b", "c", "d"), 4, feedback = Feedback(Verdict.incorrect, "x", "y"))
        val next = LearningEngine.advance(session)
        assertEquals(listOf("b", "c", "a", "d"), next.pending)
        assertEquals(0, next.completed)
        assertNull(next.feedback)
    }
    @Test fun lastMistakeStillReturnsAndCanComplete() {
        val s = Session("c@1", "L", listOf("a"), 1, feedback = Feedback(Verdict.incorrect, "x", "y"))
        val repeated = LearningEngine.advance(s)
        assertEquals(listOf("a"), repeated.pending)
        val finished = LearningEngine.advance(repeated.copy(feedback = Feedback(Verdict.correct, "x", "x")))
        assertTrue(finished.pending.isEmpty()); assertEquals(1, finished.completed)
    }
    @Test fun wrongAnswerResetsReviewInterval() {
        val result = LearningEngine.record(ExerciseProgress(attempts = 3, correct = 3, streak = 3), Verdict.incorrect, 123L)
        assertEquals(0, result.streak); assertEquals(123L, result.dueAt); assertEquals(4, result.attempts)
    }
    @Test fun successfulReviewsHaveIncreasingIntervals() {
        val first = LearningEngine.record(ExerciseProgress(), Verdict.correct, 0)
        assertEquals(LearningEngine.DAY, first.dueAt)
        val second = LearningEngine.record(first, Verdict.correct, LearningEngine.DAY)
        assertEquals(4 * LearningEngine.DAY, second.dueAt)
    }
    @Test fun typoIsAcceptedButReturnsTomorrow() {
        val p = LearningEngine.record(ExerciseProgress(streak = 4), Verdict.typo, 0)
        assertEquals(1, p.correct); assertEquals(1, p.typos); assertEquals(LearningEngine.DAY, p.dueAt)
    }
    @Test fun shortLessonsHaveEightUniqueExercises() {
        val pool = (1..30).map { word().copy(id = "e$it") }
        val selection = LearningEngine.select(pool, { ExerciseProgress() }, 0, Random(1))
        assertEquals(8, selection.size); assertEquals(8, selection.distinct().size)
    }
    @Test fun adaptationFavorsUnseenOverMastered() {
        val pool = (1..20).map { word().copy(id = "$it") }
        var unseen = 0
        repeat(100) { seed ->
            unseen += LearningEngine.select(pool, { id -> if (id.toInt() <= 10) ExerciseProgress() else ExerciseProgress(attempts = 5, correct = 5, streak = 5, dueAt = Long.MAX_VALUE) }, 0, Random(seed)).count { it.toInt() <= 10 }
        }
        assertTrue("Unseen count: $unseen", unseen > 650)
    }
}
