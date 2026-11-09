package pl.duoduo.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/** Must run on Android: host-JVM regex accepts syntax that Android's ICU rejects. */
@RunWith(AndroidJUnit4::class)
class StartupCompatibilityTest {
    @Test fun bundledCoursesLoadAndTheirAnswersCanBeGraded() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (name in listOf("language-demo.json", "programming-demo.json")) {
            val course = context.assets.open(name).bufferedReader().use { CourseValidator.parse(it.readText()) }
            assertEquals(8, course.exercises.size)
            course.exercises.forEach { exercise ->
                val response = if (exercise.answers.isNotEmpty()) Response(texts = exercise.answers.map { it.first() })
                    else Response(selected = exercise.correctOptionIds)
                assertEquals(exercise.id, Verdict.correct, LearningEngine.grade(exercise, response).verdict)
            }
        }
    }

    @Test fun numberedGapsRenderWithTheAndroidRegexEngine() {
        val values = listOf("né", "né")
        val rendered = ExerciseTemplate.gaps.replace("{{1}} questo {{2}} quello") { match ->
            values[match.groupValues[1].toInt() - 1]
        }
        assertEquals("né questo né quello", rendered)
        assertEquals(listOf("12"), ExerciseTemplate.gaps.findAll("{1} {{12}} {}").map { it.groupValues[1] }.toList())
    }

    @Test fun importValidatesGapNumberingOnAndroid() {
        val exercise = Exercise("sentence", ExerciseType.sentence_input, "Uzupełnij",
            template = "{{1}} questo {{2}} quello", answers = listOf(listOf("né"), listOf("né")))
        val course = Course(id = "regression", version = 1, title = "Regresja",
            lessons = listOf(Lesson("lesson", "Lekcja", listOf(exercise))))
        CourseValidator.validate(course)
        val wrong = course.copy(lessons = listOf(Lesson("lesson", "Lekcja", listOf(exercise.copy(template = "{{2}} questo {{1}} quello")))))
        assertThrows(IllegalArgumentException::class.java) { CourseValidator.validate(wrong) }
    }
}
