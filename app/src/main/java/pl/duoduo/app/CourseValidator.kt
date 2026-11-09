package pl.duoduo.app

import java.util.Base64
import kotlinx.serialization.json.Json

object CourseValidator {
    const val MAX_FILE_BYTES = 8 * 1024 * 1024
    val json = Json { prettyPrint = true; encodeDefaults = true }
    private val idPattern = Regex("[a-zA-Z0-9_-]{1,80}")
    private val gaps = ExerciseTemplate.gaps

    fun parse(source: String): Course {
        require(source.toByteArray().size <= MAX_FILE_BYTES) { "Plik przekracza limit 8 MB." }
        val course = try { json.decodeFromString<Course>(source) }
        catch (_: Exception) { throw IllegalArgumentException("Niepoprawny JSON lub niezgodna struktura kursu. Sprawdź wymagane pola i nazwy typów w dokumentacji.") }
        validate(course)
        return course
    }

    fun validate(course: Course) {
        require(course.schemaVersion == 1) { "Obsługiwana wersja formatu: schemaVersion = 1." }
        require(idPattern.matches(course.id) && course.version > 0) { "Kurs wymaga poprawnego id oraz dodatniej version." }
        require(course.title.isNotBlank() && course.title.length <= 150) { "Tytuł kursu musi mieć od 1 do 150 znaków." }
        require(course.description.length <= 2000) { "Opis kursu jest za długi." }
        require(course.subject in setOf("language", "programming")) { "subject musi być language lub programming." }
        require(course.lessons.isNotEmpty() && course.lessons.size <= 100) { "Kurs musi zawierać od 1 do 100 lekcji." }
        require(course.lessons.map { it.id }.distinct().size == course.lessons.size) { "Identyfikatory lekcji muszą być unikalne." }
        require(course.exercises.size <= 1000) { "Kurs może zawierać najwyżej 1000 zadań." }
        require(course.exercises.map { it.id }.distinct().size == course.exercises.size) { "Identyfikatory zadań muszą być unikalne w całym kursie." }
        course.lessons.forEach { lesson ->
            require(idPattern.matches(lesson.id) && lesson.title.isNotBlank() && lesson.title.length <= 150 && lesson.exercises.isNotEmpty()) { "Lekcja wymaga poprawnego id, tytułu i co najmniej jednego zadania." }
            lesson.exercises.forEach { exercise ->
                try { validateExercise(exercise, course.subject) }
                catch (e: IllegalArgumentException) { throw IllegalArgumentException("Zadanie ${exercise.id}: ${e.message}") }
            }
        }
    }

    private fun validateExercise(e: Exercise, subject: String) {
        require(idPattern.matches(e.id)) { "Niepoprawny identyfikator." }
        require(e.prompt.isNotBlank() && e.prompt.length <= 2000) { "Pole prompt musi mieć od 1 do 2000 znaków." }
        require(e.template.length <= 4000 && e.explanation.length <= 2000) { "Treść jest za długa." }
        require(e.options.size <= 40 && e.options.map { it.id }.distinct().size == e.options.size) { "Najwyżej 40 opcji o unikalnych id." }
        require(e.options.all { idPattern.matches(it.id) && it.text.isNotBlank() && it.text.length <= 500 }) { "Opcja wymaga id i tekstu (do 500 znaków)." }
        val input = e.type in setOf(ExerciseType.word_input, ExerciseType.sentence_input, ExerciseType.image_input)
        val image = e.type in setOf(ExerciseType.image_input, ExerciseType.image_choice)
        if (input) {
            require(e.options.isEmpty() && e.correctOptionIds.isEmpty() && !e.multiple) { "Zadanie tekstowe nie używa opcji ani multiple." }
            require(e.answers.size in 1..12 && e.answers.all { a -> a.size in 1..20 && a.all { it.isNotBlank() && it.length <= 500 } }) { "answers wymaga 1–12 luk z niepustymi listami odpowiedzi." }
            if (e.type != ExerciseType.sentence_input) require(e.answers.size == 1) { "Ten typ zadania wymaga dokładnie jednej odpowiedzi." }
            if (subject == "programming") require(!e.answerPolicy.allowTypos && e.answerPolicy.caseSensitive) { "W kursie programowania ustaw allowTypos=false i caseSensitive=true." }
        } else {
            require(e.answers.isEmpty() && e.options.size >= 2) { "Zadanie wyboru wymaga co najmniej dwóch opcji; nie używa answers." }
            require(e.correctOptionIds.isNotEmpty() && e.correctOptionIds.distinct().size == e.correctOptionIds.size && e.correctOptionIds.all { id -> e.options.any { it.id == id } }) { "correctOptionIds musi wskazywać istniejące, niepowtarzające się opcje." }
            if (e.type in setOf(ExerciseType.choice, ExerciseType.image_choice) && !e.multiple) require(e.correctOptionIds.size == 1) { "Dla wielu poprawnych odpowiedzi ustaw multiple=true." }
            if (e.type !in setOf(ExerciseType.choice, ExerciseType.image_choice)) require(!e.multiple) { "Pole multiple jest dostępne tylko dla wyboru odpowiedzi." }
        }
        if (e.type in setOf(ExerciseType.sentence_input, ExerciseType.sentence_bank)) {
            val count = if (input) e.answers.size else e.correctOptionIds.size
            val found = gaps.findAll(e.template).map { it.groupValues[1].toIntOrNull() }.toList()
            require(found == (1..count).toList()) { "template musi zawierać kolejno luki {{1}}, {{2}}, …, każdą dokładnie raz." }
            require(!gaps.replace(e.template, "").contains("{{")) { "Niepoprawny znacznik luki." }
        }
        if (image) {
            require(!e.imageAlt.isNullOrBlank() && e.imageAlt.length <= 500) { "Obrazek wymaga opisu imageAlt." }
            require(!e.imageBase64.isNullOrBlank() && e.imageBase64.length <= 2_800_000) { "Obrazek PNG/JPEG musi być zapisany w imageBase64 (do 2 MB)." }
            val bytes = try { Base64.getDecoder().decode(e.imageBase64) } catch (_: Exception) { throw IllegalArgumentException("Niepoprawny Base64 obrazka.") }
            val png = bytes.size >= 8 && bytes.take(8).map { it.toInt() and 255 } == listOf(137, 80, 78, 71, 13, 10, 26, 10)
            val jpeg = bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte()
            require(bytes.size <= 2 * 1024 * 1024 && (png || jpeg)) { "Dozwolone są obrazki PNG lub JPEG do 2 MB." }
        } else require(e.imageBase64 == null && e.imageAlt == null) { "Pola obrazka są dostępne tylko dla zadań obrazkowych." }
    }
}
