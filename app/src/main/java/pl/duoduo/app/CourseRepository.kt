package pl.duoduo.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AtomicFile
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.Base64
import kotlinx.serialization.encodeToString

class CourseRepository(private val context: Context) {
    private val file = AtomicFile(File(context.filesDir, "learning-state.json"))

    fun load(): LocalState {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) {
            val courses = listOf("language-demo.json", "programming-demo.json").map { name ->
                CourseValidator.parse(context.assets.open(name).bufferedReader().use { it.readText() })
            }
            return LocalState(courses = courses).also(::save)
        }
        return CourseValidator.json.decodeFromString<LocalState>(file.readFully().decodeToString())
    }

    fun save(state: LocalState) {
        val bytes = CourseValidator.json.encodeToString(state).toByteArray()
        val stream = file.startWrite()
        try { stream.write(bytes); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }

    fun import(uri: Uri): Course {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (output.size() <= CourseValidator.MAX_FILE_BYTES) {
                val read = input.read(buffer, 0, minOf(buffer.size, CourseValidator.MAX_FILE_BYTES + 1 - output.size()))
                if (read < 0) break
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
            ?: throw IllegalArgumentException("Nie można otworzyć wybranego pliku.")
        require(bytes.size <= CourseValidator.MAX_FILE_BYTES) { "Plik przekracza limit 8 MB." }
        return CourseValidator.parse(bytes.decodeToString()).also { course ->
            course.exercises.filter { it.imageBase64 != null }.forEach { e ->
                val image = Base64.getDecoder().decode(e.imageBase64)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(image, 0, image.size, bounds)
                require(bounds.outWidth in 1..2048 && bounds.outHeight in 1..2048) { "Zadanie ${e.id}: obrazek musi mieć od 1 do 2048 pikseli na każdy wymiar." }
                val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
                require(bitmap != null) { "Zadanie ${e.id}: nie można odczytać obrazka." }
                bitmap.recycle()
            }
        }
    }
}
