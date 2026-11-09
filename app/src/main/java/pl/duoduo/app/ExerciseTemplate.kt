package pl.duoduo.app

/** Shared by import validation and rendering; ICU requires literal closing braces to be escaped. */
internal object ExerciseTemplate {
    val gaps = Regex("""\{\{(\d+)\}\}""")
}
