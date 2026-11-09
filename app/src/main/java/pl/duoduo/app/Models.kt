package pl.duoduo.app

import kotlinx.serialization.Serializable

@Serializable
enum class ExerciseType {
    choice, word_input, sentence_input, sentence_bank,
    sentence_select, sentence_order, image_input, image_choice
}

@Serializable
data class Choice(val id: String, val text: String)

@Serializable
data class AnswerPolicy(
    val allowTypos: Boolean = true,
    val caseSensitive: Boolean = false,
    val trim: Boolean = true
)

@Serializable
data class Course(
    val schemaVersion: Int = 1,
    val id: String,
    val version: Int,
    val title: String,
    val description: String = "",
    val subject: String = "language",
    val lessons: List<Lesson>
) {
    val exercises: List<Exercise> get() = lessons.flatMap { it.exercises }
    val key: String get() = "$id@$version"
}

@Serializable
data class Lesson(val id: String, val title: String, val exercises: List<Exercise>)

@Serializable
data class Exercise(
    val id: String,
    val type: ExerciseType,
    val prompt: String,
    val template: String = "",
    val options: List<Choice> = emptyList(),
    val correctOptionIds: List<String> = emptyList(),
    val answers: List<List<String>> = emptyList(),
    val multiple: Boolean = false,
    val imageBase64: String? = null,
    val imageAlt: String? = null,
    val explanation: String = "",
    val answerPolicy: AnswerPolicy = AnswerPolicy()
)

@Serializable
data class ExerciseProgress(
    val attempts: Int = 0,
    val correct: Int = 0,
    val typos: Int = 0,
    val streak: Int = 0,
    val dueAt: Long = 0
)

@Serializable
data class Response(val texts: List<String> = emptyList(), val selected: List<String> = emptyList())

@Serializable
enum class Verdict { correct, typo, incorrect }

@Serializable
data class Feedback(val verdict: Verdict, val expected: String, val submitted: String)

@Serializable
data class Session(
    val courseKey: String,
    val title: String,
    val pending: List<String>,
    val total: Int,
    val completed: Int = 0,
    val mistakes: Int = 0,
    val typos: Int = 0,
    val feedback: Feedback? = null,
    val response: Response = Response(),
    val optionOrder: List<String> = emptyList()
)

@Serializable
data class LocalState(
    val courses: List<Course> = emptyList(),
    val progress: Map<String, ExerciseProgress> = emptyMap(),
    val session: Session? = null
)
