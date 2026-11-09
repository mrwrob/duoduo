package pl.duoduo.app

import java.io.File
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class CourseValidatorTest {
    private fun course() = Course(id = "demo", version = 1, title = "Demo", lessons = listOf(Lesson("lesson", "Lekcja", listOf(Exercise("word", ExerciseType.word_input, "Wpisz", answers = listOf(listOf("water")))))))
    private fun invalid(c: Course) { assertThrows(IllegalArgumentException::class.java) { CourseValidator.validate(c) } }
    @Test fun validCourseRoundTrips() { val c = course(); assertEquals(c, CourseValidator.parse(CourseValidator.json.encodeToString(c))) }
    @Test fun unknownSchemaIsRejected() { invalid(course().copy(schemaVersion = 2)) }
    @Test fun duplicatesAcrossLessonsAreRejected() { val c = course(); invalid(c.copy(lessons = c.lessons + c.lessons.first().copy(id = "second"))) }
    @Test fun emptyAnswersAreRejected() { val c = course(); invalid(c.copy(lessons = listOf(c.lessons.first().copy(exercises = listOf(c.exercises.first().copy(answers = emptyList())))))) }
    @Test fun unknownOptionReferencesAreRejected() {
        val c = course()
        val e = Exercise("x", ExerciseType.choice, "Wybierz", options = listOf(Choice("a", "A"), Choice("b", "B")), correctOptionIds = listOf("missing"))
        invalid(c.copy(lessons = listOf(Lesson("l", "L", listOf(e)))))
    }
    @Test fun gapsMustMatchAnswers() {
        val c = course(); val e = c.exercises.first().copy(type = ExerciseType.sentence_input, template = "{{2}}")
        invalid(c.copy(lessons = listOf(Lesson("l", "L", listOf(e)))))
    }
    @Test fun codeRequiresStrictAnswerPolicy() { invalid(course().copy(subject = "programming")) }
    @Test fun malformedAndUnknownFieldsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { CourseValidator.parse("{bad}") }
        val encoded = CourseValidator.json.encodeToString(course()).replace("\"schemaVersion\"", "\"typoField\"")
        assertThrows(IllegalArgumentException::class.java) { CourseValidator.parse(encoded) }
    }
    @Test fun availableCoursesCoverAllVariantsAndAcceptTheirAnswers() {
        val allTypes = mutableSetOf<ExerciseType>()
        val bundled = listOf("language-demo.json", "programming-demo.json").map { File("src/main/assets/$it") }
        val imports = File("../courses").listFiles().orEmpty().filter { it.extension == "json" && !it.name.endsWith(".schema.json") }
        (bundled + imports).forEach { file ->
            val c = CourseValidator.parse(file.readText())
            c.exercises.forEach { e ->
                allTypes += e.type
                val response = if (e.answers.isNotEmpty()) Response(texts = e.answers.map { it.first() }) else Response(selected = e.correctOptionIds)
                assertEquals("${c.id}/${e.id}", Verdict.correct, LearningEngine.grade(e, response).verdict)
                e.answers.forEachIndexed { index, variants ->
                    variants.forEach { variant ->
                        val texts = response.texts.toMutableList().also { it[index] = variant }
                        assertEquals("${c.id}/${e.id}: $variant", Verdict.correct, LearningEngine.grade(e, Response(texts = texts)).verdict)
                    }
                }
            }
        }
        assertEquals(ExerciseType.entries.toSet(), allTypes)
    }
    @Test fun stateRestoresAnUnfinishedAnswerWithoutRegrading() {
        val c = course()
        val state = LocalState(courses = listOf(c), session = Session(c.key, "Lekcja", listOf("word"), 1,
            feedback = Feedback(Verdict.typo, "water", "wator"), response = Response(texts = listOf("wator"))))
        assertEquals(state, CourseValidator.json.decodeFromString<LocalState>(CourseValidator.json.encodeToString(state)))
    }
}
