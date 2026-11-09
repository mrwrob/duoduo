package pl.duoduo.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "pl-rPL-w411dp-h891dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun completeEveryVariantAndRestoreMistakeFeedback() {
        val vm = ViewModelProvider(compose.activity)[LearningViewModel::class.java]
        await { vm.ready && !vm.busy }
        compose.onNodeWithText("duoduo").assertIsDisplayed()
        screenshot("home")
        val course = vm.state.courses.first { it.subject == "language" }
        compose.runOnIdle { vm.start(course, course.lessons.first()) }
        await { !vm.busy }
        assertNull(vm.error)
        assertTrue(vm.inLesson)
        var testedMistake = false
        var testedTypo = false
        val seen = mutableSetOf<ExerciseType>()
        var safety = 0
        while (vm.state.session!!.pending.isNotEmpty() && safety++ < 12) {
            val exercise = course.exercises.first { it.id == vm.state.session!!.pending.first() }
            seen += exercise.type
            if (exercise.type == ExerciseType.word_input && !testedMistake) {
                compose.onNodeWithTag("answer-0").performScrollTo().performTextInput("wrong")
                compose.onNodeWithTag("check-next").performClick()
                await { vm.state.session?.feedback != null && !vm.busy }
                compose.onNodeWithText("Jeszcze to przećwiczymy").assertIsDisplayed()
                val restored = CourseRepository(compose.activity).load()
                assertEquals(Verdict.incorrect, restored.session?.feedback?.verdict)
                assertEquals(1, restored.progress[LearningEngine.progressKey(course, exercise.id)]?.attempts)
                screenshot("feedback-error")
                testedMistake = true
            } else {
                if (exercise.answers.isNotEmpty()) {
                    exercise.answers.forEachIndexed { index, answers ->
                        val value = if (exercise.type == ExerciseType.word_input && !testedTypo) "wator" else answers.first()
                        compose.onNodeWithTag("answer-$index").performScrollTo().performTextInput(value)
                    }
                } else exercise.correctOptionIds.forEach { id -> compose.onNodeWithTag("option-$id").performScrollTo().performClick() }
                screenshot("exercise-${exercise.type}")
                compose.onNodeWithTag("check-next").performClick()
                await { vm.state.session?.feedback != null && !vm.busy }
                val verdict = vm.state.session!!.feedback!!.verdict
                if (exercise.type == ExerciseType.word_input && !testedTypo) {
                    assertEquals(Verdict.typo, verdict)
                    compose.onNodeWithText("Zaliczone — drobna literówka").assertIsDisplayed()
                    testedTypo = true
                    screenshot("feedback-typo")
                } else assertEquals(exercise.id, Verdict.correct, verdict)
            }
            compose.onNodeWithTag("check-next").performClick()
            await { vm.state.session?.feedback == null && !vm.busy }
        }
        assertEquals(ExerciseType.entries.toSet(), seen)
        assertTrue(testedMistake && testedTypo)
        assertTrue(vm.state.session!!.pending.isEmpty())
        compose.onNodeWithText("Krok do przodu!").assertIsDisplayed()
        screenshot("completed")
        compose.onNodeWithText("Wróć do kursów").performScrollTo().performClick()
        await { vm.state.session == null && !vm.busy }
        assertEquals(8, vm.state.progress.values.count { it.correct > 0 })
    }

    private fun await(condition: () -> Boolean) {
        compose.waitUntil(20_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val file = File("build/screenshots/$name.png").also { it.parentFile?.mkdirs() }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            file.outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
            bitmap.recycle()
        }
    }
}
