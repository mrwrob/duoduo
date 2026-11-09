package pl.duoduo.app

import java.text.Normalizer
import java.util.Locale
import kotlin.math.ln
import kotlin.random.Random

object LearningEngine {
    const val DAY = 86_400_000L
    fun progressKey(course: Course, exerciseId: String) = "${course.key}/$exerciseId"

    private fun normalize(text: String, policy: AnswerPolicy): String {
        var result = Normalizer.normalize(text, Normalizer.Form.NFC)
        if (policy.trim) result = result.trim()
        if (!policy.caseSensitive) result = result.lowercase(Locale.ROOT)
        return result
    }

    // One insertion, deletion, replacement or adjacent transposition. Short words stay exact.
    internal fun minorTypo(a: String, b: String): Boolean {
        val left = a.codePoints().toArray()
        val right = b.codePoints().toArray()
        if (minOf(left.size, right.size) < 4 || kotlin.math.abs(left.size - right.size) > 1) return false
        if (left.size == right.size) {
            val mismatch = left.indices.filter { left[it] != right[it] }
            return mismatch.size == 1 || (mismatch.size == 2 && mismatch[1] == mismatch[0] + 1 &&
                left[mismatch[0]] == right[mismatch[1]] && left[mismatch[1]] == right[mismatch[0]])
        }
        val shorter = if (left.size < right.size) left else right
        val longer = if (left.size < right.size) right else left
        var i = 0
        var j = 0
        var skipped = false
        while (i < shorter.size && j < longer.size) {
            if (shorter[i] == longer[j]) { i++; j++ }
            else { if (skipped) return false; skipped = true; j++ }
        }
        return true
    }

    fun grade(exercise: Exercise, response: Response): Feedback {
        val textType = exercise.type in setOf(ExerciseType.word_input, ExerciseType.sentence_input, ExerciseType.image_input)
        if (!textType) {
            val ordered = exercise.type in setOf(ExerciseType.sentence_bank, ExerciseType.sentence_order)
            val valid = response.selected.distinct().size == response.selected.size &&
                if (ordered) response.selected == exercise.correctOptionIds
                else response.selected.toSet() == exercise.correctOptionIds.toSet()
            val lookup = exercise.options.associate { it.id to it.text }
            return Feedback(if (valid) Verdict.correct else Verdict.incorrect,
                exercise.correctOptionIds.joinToString(" · ") { lookup[it].orEmpty() },
                response.selected.joinToString(" · ") { lookup[it].orEmpty() })
        }
        val policy = exercise.answerPolicy
        val results = exercise.answers.mapIndexed { index, alternatives ->
            val typed = normalize(response.texts.getOrNull(index).orEmpty(), policy)
            val exact = alternatives.firstOrNull { normalize(it, policy) == typed }
            val close = if (policy.allowTypos && typed.isNotBlank()) alternatives.firstOrNull { minorTypo(typed, normalize(it, policy)) } else null
            when {
                exact != null -> Verdict.correct to exact
                close != null -> Verdict.typo to close
                else -> Verdict.incorrect to alternatives.first()
            }
        }
        val verdict = when {
            response.texts.size != exercise.answers.size || results.any { it.first == Verdict.incorrect } -> Verdict.incorrect
            results.any { it.first == Verdict.typo } -> Verdict.typo
            else -> Verdict.correct
        }
        return Feedback(verdict, results.joinToString(" · ") { it.second }, response.texts.joinToString(" · "))
    }

    fun record(old: ExerciseProgress, verdict: Verdict, now: Long): ExerciseProgress {
        if (verdict == Verdict.incorrect) return old.copy(attempts = old.attempts + 1, streak = 0, dueAt = now)
        val streak = if (verdict == Verdict.typo) 1 else old.streak + 1
        val intervals = listOf(1L, 3L, 7L, 14L, 30L)
        return old.copy(attempts = old.attempts + 1, correct = old.correct + 1,
            typos = old.typos + if (verdict == Verdict.typo) 1 else 0,
            streak = streak, dueAt = now + intervals[(streak - 1).coerceAtMost(4)] * DAY)
    }

    fun select(exercises: List<Exercise>, progress: (String) -> ExerciseProgress, now: Long, random: Random = Random.Default): List<String> =
        exercises.sortedBy { exercise ->
            val p = progress(exercise.id)
            val weight = when {
                p.attempts == 0 -> 6.0
                p.dueAt <= now -> 4.0 + (p.attempts - p.correct).coerceAtMost(5)
                else -> 1.0 / (p.streak + 1)
            }
            -ln(random.nextDouble().coerceAtLeast(0.000001)) / weight
        }.take(8).map { it.id }

    fun advance(session: Session): Session {
        val feedback = requireNotNull(session.feedback)
        val remaining = session.pending.drop(1).toMutableList()
        if (feedback.verdict == Verdict.incorrect) remaining.add(minOf(2, remaining.size), session.pending.first())
        return session.copy(pending = remaining,
            completed = session.completed + if (feedback.verdict == Verdict.incorrect) 0 else 1,
            feedback = null, response = Response(), optionOrder = emptyList())
    }
}
