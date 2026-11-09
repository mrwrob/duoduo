package pl.duoduo.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CourseRepository(application)
    var state by mutableStateOf(LocalState()); private set
    var busy by mutableStateOf(true); private set
    var ready by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var importPreview by mutableStateOf<Course?>(null); private set
    var inLesson by mutableStateOf(false); private set
    var notice by mutableStateOf<String?>(null); private set

    init {
        viewModelScope.launch {
            try { state = withContext(Dispatchers.IO) { repository.load() }; ready = true }
            catch (_: Exception) { error = "Nie udało się odczytać danych. Zamknij i uruchom aplikację ponownie. Zapisane dane nie zostały nadpisane." }
            finally { busy = false }
        }
    }

    private fun commit(transform: (LocalState) -> LocalState, after: () -> Unit = {}) {
        if (busy || !ready) return
        busy = true
        viewModelScope.launch {
            try {
                val updated = transform(state)
                withContext(Dispatchers.IO) { repository.save(updated) }
                state = updated
                after()
            } catch (e: Exception) { error = e.message ?: "Nie udało się zapisać zmian. Spróbuj ponownie." }
            finally { busy = false }
        }
    }

    fun clearError() { error = null }
    fun dismissImport() { importPreview = null }
    fun previewImport(uri: Uri) {
        if (busy || !ready) return
        busy = true
        viewModelScope.launch {
            try { importPreview = withContext(Dispatchers.IO) { repository.import(uri) } }
            catch (e: Exception) { error = e.message ?: "Nie udało się zaimportować kursu." }
            finally { busy = false }
        }
    }

    fun confirmImport() {
        val course = importPreview ?: return
        commit({ old ->
            val existing = old.courses.find { it.id == course.id }
            require(existing == null || course.version > existing.version || course == existing) { "Ten identyfikator kursu już istnieje. Zmieniona treść wymaga większej wartości version." }
            require(existing != null || old.courses.size < 20) { "Limit pierwszej wersji: 20 kursów." }
            val retained = old.courses.filterNot { it.id == course.id }
            require(retained.sumOf { c -> c.exercises.sumOf { it.imageBase64?.length ?: 0 } } + course.exercises.sumOf { it.imageBase64?.length ?: 0 } <= 24 * 1024 * 1024) { "Przekroczono łączny limit obrazków (24 MB Base64)." }
            old.copy(courses = retained + course,
                progress = if (existing != null && existing.version != course.version) old.progress.filterKeys { !it.startsWith("${existing.key}/") } else old.progress,
                session = old.session.takeUnless { existing != null && existing.version != course.version && it?.courseKey == existing.key })
        }, { importPreview = null; notice = "Zaimportowano: ${course.title}" })
    }

    fun progress(course: Course, id: String) = state.progress[LearningEngine.progressKey(course, id)] ?: ExerciseProgress()
    fun due(course: Course, now: Long = System.currentTimeMillis()) = course.exercises.filter { val p = progress(course, it.id); p.attempts > 0 && p.dueAt <= now }

    fun start(course: Course, lesson: Lesson? = null) {
        if (state.session != null) { error = "Najpierw dokończ lub zakończ zapisaną lekcję."; return }
        val now = System.currentTimeMillis()
        val pool = lesson?.exercises ?: due(course, now)
        if (pool.isEmpty()) { notice = "Brak powtórek na teraz."; return }
        val ids = LearningEngine.select(pool, { progress(course, it) }, now)
        val first = course.exercises.first { it.id == ids.first() }
        commit({ it.copy(session = Session(course.key, lesson?.title ?: "Powtórka", ids, ids.size, optionOrder = first.options.map { it.id }.shuffled())) }, { inLesson = true })
    }

    fun resume() { inLesson = true }
    fun pause() { commit({ it }, { inLesson = false }) }
    fun finish() { commit({ it.copy(session = null) }, { inLesson = false }) }
    fun updateResponse(response: Response) {
        val session = state.session ?: return
        if (!busy && session.feedback == null) state = state.copy(session = session.copy(response = response))
    }

    fun submit() {
        val session = state.session ?: return
        if (session.feedback != null || session.pending.isEmpty()) return
        val course = state.courses.first { it.key == session.courseKey }
        val exercise = course.exercises.first { it.id == session.pending.first() }
        val feedback = LearningEngine.grade(exercise, session.response)
        val key = LearningEngine.progressKey(course, exercise.id)
        commit({ old -> old.copy(
            progress = old.progress + (key to LearningEngine.record(old.progress[key] ?: ExerciseProgress(), feedback.verdict, System.currentTimeMillis())),
            session = session.copy(feedback = feedback,
                mistakes = session.mistakes + if (feedback.verdict == Verdict.incorrect) 1 else 0,
                typos = session.typos + if (feedback.verdict == Verdict.typo) 1 else 0)
        ) })
    }

    fun next() {
        val session = state.session ?: return
        if (session.feedback == null) return
        val advanced = LearningEngine.advance(session)
        val course = state.courses.first { it.key == session.courseKey }
        val order = course.exercises.find { it.id == advanced.pending.firstOrNull() }?.options?.map { it.id }?.shuffled().orEmpty()
        commit({ it.copy(session = advanced.copy(optionOrder = order)) })
    }
}
