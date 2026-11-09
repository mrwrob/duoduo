package pl.duoduo.app

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Base64

private val Forest = Color(0xFF1C5646)
private val Lime = Color(0xFFD5F47B)
private val Paper = Color(0xFFF6F8F4)
private val Ink = Color(0xFF20382F)
private val Muted = Color(0xFF65756D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Forest, onPrimary = Color.White, primaryContainer = Lime, onPrimaryContainer = Ink,
                background = Paper, onBackground = Ink, surface = Paper, onSurface = Ink, secondary = Forest,
                secondaryContainer = Color(0xFFEAF0DD), onSecondaryContainer = Ink,
                surfaceVariant = Color(0xFFE7EDE4), onSurfaceVariant = Muted
            )) { DuoApp() }
        }
    }
}

@Composable
fun DuoApp(vm: LearningViewModel = viewModel()) {
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::previewImport) }
    val session = vm.state.session
    var exitDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = vm.inLesson && !vm.busy) { exitDialog = true }
    if (vm.inLesson && session != null) LessonScreen(vm, session, onExit = { exitDialog = true })
    else HomeScreen(vm, onImport = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })

    if (exitDialog) AlertDialog(
        onDismissRequest = { if (!vm.busy) exitDialog = false },
        title = { Text("Zrobimy przerwę?") },
        text = { Text("Odpowiedzi i kolejka ćwiczeń zostaną zapisane. Możesz wrócić do lekcji później.") },
        confirmButton = { TextButton(enabled = !vm.busy, onClick = { exitDialog = false; vm.pause() }) { Text("Zapisz i wróć") } },
        dismissButton = { TextButton(onClick = { exitDialog = false }) { Text("Uczę się dalej") } }
    )
    vm.error?.let { message -> AlertDialog(onDismissRequest = vm::clearError,
        title = { Text("Nie udało się wykonać operacji") }, text = { Text(message) },
        confirmButton = { TextButton(onClick = vm::clearError) { Text("Rozumiem") } }) }
    vm.importPreview?.let { course ->
        val previous = vm.state.courses.find { it.id == course.id }
        AlertDialog(onDismissRequest = { if (!vm.busy) vm.dismissImport() },
            title = { Text("Import kursu") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(course.title, fontWeight = FontWeight.Bold)
                Text("${course.lessons.size} lekcji · ${course.exercises.size} ćwiczeń\nWersja ${course.version}")
                Text(course.description)
                if (previous != null && previous.version != course.version) Text("Ta wersja zastąpi poprzedni kurs i rozpocznie jego postęp od nowa.", color = MaterialTheme.colorScheme.error)
                Text("Kurs będzie dostępny bez internetu.", color = Muted)
            } },
            confirmButton = { TextButton(enabled = !vm.busy, onClick = vm::confirmImport) { Text("Importuj") } },
            dismissButton = { TextButton(enabled = !vm.busy, onClick = vm::dismissImport) { Text("Anuluj") } })
    }
}

@Composable
private fun HomeScreen(vm: LearningViewModel, onImport: () -> Unit) {
    var reviewTab by rememberSaveable { mutableStateOf(false) }
    var discardDialog by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); kotlinx.coroutines.delay(30_000) } }
    Scaffold(containerColor = Paper) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("duoduo", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Forest, letterSpacing = (-1).sp)
                Surface(shape = RoundedCornerShape(30.dp), color = Color(0xFFE7EDE4)) { Text("●  OFFLINE", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Forest) }
            }
            Surface(color = Forest, shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("MAŁE KROKI. NOWE UMIEJĘTNOŚCI.", color = Lime, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Text("Dziś też możesz\nnauczyć się czegoś.", color = Color.White, fontSize = 29.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
                    Text("Krótka lekcja. Jedna rzecz więcej.", color = Color(0xFFD5E5DD), fontSize = 14.sp)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.18f), modifier = Modifier.padding(vertical = 4.dp))
                    val practiced = vm.state.progress.values.count { it.attempts > 0 }
                    val due = vm.state.courses.sumOf { vm.due(it, now).size }
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        Column { Text("$practiced", color = Lime, fontWeight = FontWeight.Bold, fontSize = 24.sp); Text("ćwiczeń poznanych", color = Color.White, fontSize = 12.sp) }
                        Column { Text("$due", color = Lime, fontWeight = FontWeight.Bold, fontSize = 24.sp); Text("do powtórki", color = Color.White, fontSize = 12.sp) }
                    }
                }
            }
            if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Forest)
            vm.notice?.let { Text(it, color = Forest, style = MaterialTheme.typography.bodySmall) }
            vm.state.session?.let { saved ->
                Surface(color = Color(0xFFEAF0DD), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (saved.pending.isEmpty()) "Lekcja ukończona" else "Twoja lekcja czeka", fontWeight = FontWeight.Bold)
                        Text("${saved.title} · ${saved.completed}/${saved.total} ćwiczeń")
                        Button(onClick = vm::resume, enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) { Text(if (saved.pending.isEmpty()) "Zobacz podsumowanie" else "Kontynuuj lekcję") }
                        if (saved.pending.isNotEmpty()) TextButton(onClick = { discardDialog = true }, enabled = !vm.busy) { Text("Zakończ tę lekcję") }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = !reviewTab, onClick = { reviewTab = false }, label = { Text("Moje kursy") })
                FilterChip(selected = reviewTab, onClick = { reviewTab = true }, label = { Text("Powtórki") })
            }
            vm.state.courses.forEach { course ->
                CourseCard(course, vm, reviewTab, now)
            }
            if (vm.state.courses.isEmpty() && vm.ready) Text("Dodaj pierwszy kurs z pliku JSON.")
            OutlinedButton(onClick = onImport, enabled = !vm.busy && vm.ready, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(16.dp)) { Text("+  Importuj kurs z JSON", fontWeight = FontWeight.Bold) }
            Text("Twój rytm nauki\nĆwiczenia dobieramy losowo, częściej wracając do tych, które wymagają utrwalenia.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(12.dp))
        }
    }
    if (discardDialog) AlertDialog(onDismissRequest = { discardDialog = false }, title = { Text("Zakończyć lekcję?") },
        text = { Text("Usuniemy kolejkę pozostałych ćwiczeń. Wyniki udzielonych odpowiedzi zostaną zachowane.") },
        confirmButton = { TextButton(onClick = { discardDialog = false; vm.finish() }) { Text("Zakończ") } },
        dismissButton = { TextButton(onClick = { discardDialog = false }) { Text("Anuluj") } })
}

@Composable
private fun CourseCard(course: Course, vm: LearningViewModel, review: Boolean, now: Long) {
    var expanded by rememberSaveable(course.key) { mutableStateOf(false) }
    val due = vm.due(course, now)
    val learned = course.exercises.count { vm.progress(course, it.id).correct > 0 }
    Surface(color = Color.White, shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E7DD))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = if (course.subject == "programming") Color(0xFFEBE6F6) else Color(0xFFEAF0DD), shape = RoundedCornerShape(14.dp)) {
                    Text(if (course.subject == "programming") "</>" else "Aa", Modifier.padding(14.dp), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Forest)
                }
                Column(Modifier.weight(1f)) {
                    Text(if (course.subject == "programming") "PROGRAMOWANIE" else "JĘZYK OBCY", fontSize = 10.sp, letterSpacing = 1.sp, color = Muted)
                    Text(course.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(course.description, color = Muted, fontSize = 13.sp)
            LinearProgressIndicator(progress = { learned.toFloat() / course.exercises.size }, modifier = Modifier.fillMaxWidth().height(6.dp), trackColor = Color(0xFFEDF1E9))
            Text("$learned z ${course.exercises.size} ćwiczeń zaliczonych", fontSize = 12.sp, color = Muted)
            if (review) {
                Text(if (due.isEmpty()) "Wszystko powtórzone na teraz." else "Czeka na Ciebie ${due.size} ćwiczeń.", fontWeight = FontWeight.Medium)
                val next = course.exercises.map { vm.progress(course, it.id) }.filter { it.attempts > 0 && it.dueAt > now }.minOfOrNull { it.dueAt }
                if (due.isEmpty() && next != null) Text("Najbliższa powtórka: ${java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(next))}", color = Muted, fontSize = 12.sp)
                if (due.isEmpty() && next == null) Text("Zacznij lekcję, aby zaplanować powtórki.", color = Muted, fontSize = 12.sp)
                Button(onClick = { vm.start(course) }, enabled = due.isNotEmpty() && !vm.busy && vm.state.session == null, modifier = Modifier.fillMaxWidth()) { Text("Powtórz teraz") }
            } else {
                OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) { Text(if (expanded) "Zwiń lekcje" else "Wybierz lekcję · ${course.lessons.size}") }
                if (expanded) course.lessons.forEachIndexed { index, lesson ->
                    FilledTonalButton(onClick = { vm.start(course, lesson) }, enabled = !vm.busy && vm.state.session == null, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text("${index + 1}. ${lesson.title}", fontWeight = FontWeight.Bold)
                            Text("Do ${minOf(8, lesson.exercises.size)} ćwiczeń w sesji", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonScreen(vm: LearningViewModel, session: Session, onExit: () -> Unit) {
    val course = vm.state.courses.first { it.key == session.courseKey }
    if (session.pending.isEmpty()) {
        Scaffold(containerColor = Paper) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(44.dp))
                Surface(color = Lime, shape = RoundedCornerShape(32.dp)) { Text("✓", Modifier.padding(32.dp), fontSize = 56.sp, color = Forest) }
                Text("Krok do przodu!", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Lekcja ukończona. Wróć jutro, aby utrwalić to, czego się uczysz.", color = Muted)
                Text("Zaliczone ćwiczenia: ${session.completed}\nBłędne próby: ${session.mistakes}\nZaakceptowane literówki: ${session.typos}", lineHeight = 30.sp)
                Button(onClick = vm::finish, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text("Wróć do kursów") }
            }
        }
        return
    }
    val exercise = course.exercises.first { it.id == session.pending.first() }
    val keyboard = LocalSoftwareKeyboardController.current
    val feedback = session.feedback
    val response = session.response
    val textType = exercise.type in setOf(ExerciseType.word_input, ExerciseType.sentence_input, ExerciseType.image_input)
    val ready = if (textType) response.texts.size == exercise.answers.size && response.texts.all { it.isNotBlank() }
        else if (exercise.type in setOf(ExerciseType.sentence_bank, ExerciseType.sentence_order)) response.selected.size == exercise.correctOptionIds.size
        else response.selected.isNotEmpty()
    Scaffold(containerColor = Paper, bottomBar = {
        Surface(color = if (feedback == null) Paper else when (feedback.verdict) {
            Verdict.correct -> Color(0xFFEAF3DF); Verdict.typo -> Color(0xFFFFF1D3); Verdict.incorrect -> Color(0xFFFFE9E3)
        }, shadowElevation = 4.dp) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (feedback != null) {
                    Column(Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState()).semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(when (feedback.verdict) { Verdict.correct -> "Dobrze!"; Verdict.typo -> "Zaliczone — drobna literówka"; Verdict.incorrect -> "Jeszcze to przećwiczymy" }, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (feedback.verdict != Verdict.correct) {
                            Text("Twoja odpowiedź: ${feedback.submitted}", fontSize = 13.sp)
                            Text("Poprawnie: ${feedback.expected}", fontWeight = FontWeight.SemiBold)
                        }
                        if (exercise.explanation.isNotBlank()) Text(exercise.explanation, fontSize = 13.sp)
                    }
                }
                Button(onClick = { keyboard?.hide(); if (feedback == null) vm.submit() else vm.next() }, enabled = !vm.busy && (feedback != null || ready), modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("check-next"), shape = RoundedCornerShape(16.dp)) {
                    Text(if (vm.busy) "Zapisuję…" else if (feedback == null) "Sprawdź" else "Dalej", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }) { padding ->
        key(exercise.id, session.completed, session.mistakes) {
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    TextButton(onClick = onExit, enabled = !vm.busy, contentPadding = PaddingValues(0.dp)) { Text("Zamknij") }
                    LinearProgressIndicator(progress = { session.completed.toFloat() / session.total }, modifier = Modifier.weight(1f).height(8.dp))
                    Text("${session.completed}/${session.total}", fontSize = 12.sp, color = Muted)
                }
                Text(session.title.uppercase(), color = Forest, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                Text(exercise.prompt, fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                Text(instruction(exercise), color = Muted, fontSize = 13.sp)
                ExerciseContent(exercise, session, course.subject == "programming", enabled = feedback == null && !vm.busy, onChange = vm::updateResponse)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private fun instruction(e: Exercise) = when (e.type) {
    ExerciseType.choice, ExerciseType.image_choice -> if (e.multiple) "Zaznacz wszystkie poprawne odpowiedzi." else "Wybierz jedną odpowiedź."
    ExerciseType.word_input -> "Wpisz całe brakujące słowo."
    ExerciseType.sentence_input -> "Uzupełnij ponumerowane luki."
    ExerciseType.sentence_bank -> "Dotykaj słów, aby kolejno uzupełniać luki. Dotknij odpowiedzi, aby ją usunąć."
    ExerciseType.sentence_select -> "Zaznacz pasujące słowa w zdaniu."
    ExerciseType.sentence_order -> "Ułóż zdanie, dotykając kafelków w odpowiedniej kolejności."
    ExerciseType.image_input -> "Wpisz słowo pasujące do obrazka."
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseContent(e: Exercise, session: Session, code: Boolean, enabled: Boolean, onChange: (Response) -> Unit) {
    val response = session.response
    val font = if (code) FontFamily.Monospace else FontFamily.Default
    e.imageBase64?.let { encoded ->
        val bitmap = remember(encoded) { val bytes = Base64.getDecoder().decode(encoded); BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
        if (bitmap != null) Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Image(bitmap = bitmap, contentDescription = e.imageAlt, modifier = Modifier.fillMaxWidth().height(190.dp).padding(20.dp))
        }
    }
    if (e.template.isNotBlank()) {
        val rendered = ExerciseTemplate.gaps.replace(e.template) { match ->
            val index = match.groupValues[1].toInt() - 1
            val filled = if (e.type == ExerciseType.sentence_bank) e.options.find { it.id == response.selected.getOrNull(index) }?.text else response.texts.getOrNull(index)
            if (filled.isNullOrBlank()) "[${index + 1}: ____]" else "[$filled]"
        }
        Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) { Text(rendered, Modifier.fillMaxWidth().padding(20.dp), fontFamily = font, fontSize = 19.sp, lineHeight = 30.sp) }
    }
    when (e.type) {
        ExerciseType.word_input, ExerciseType.sentence_input, ExerciseType.image_input -> {
            e.answers.indices.forEach { index ->
                OutlinedTextField(value = response.texts.getOrNull(index).orEmpty(), onValueChange = { value ->
                    val updated = e.answers.indices.map { response.texts.getOrNull(it).orEmpty() }.toMutableList()
                    updated[index] = value.take(500)
                    onChange(response.copy(texts = updated))
                }, modifier = Modifier.fillMaxWidth().testTag("answer-$index"), enabled = enabled, singleLine = true,
                    label = { Text(if (e.answers.size == 1) "Twoja odpowiedź" else "Luka ${index + 1}") },
                    shape = RoundedCornerShape(14.dp), textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = font),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false))
            }
        }
        ExerciseType.choice, ExerciseType.image_choice -> {
            val options = session.optionOrder.mapNotNull { id -> e.options.find { it.id == id } }
            options.forEach { option ->
                val selected = option.id in response.selected
                Surface(color = if (selected) Color(0xFFEAF0DD) else Color.White, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Forest else Color(0xFFDDE5D9))) {
                    Row(Modifier.fillMaxWidth().testTag("option-${option.id}").toggleable(value = selected, enabled = enabled, role = if (e.multiple) Role.Checkbox else Role.RadioButton, onValueChange = {
                        val ids = if (!e.multiple) listOf(option.id) else if (selected) response.selected - option.id else response.selected + option.id
                        onChange(response.copy(selected = ids))
                    }).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (e.multiple) Checkbox(checked = selected, onCheckedChange = null, enabled = enabled) else RadioButton(selected = selected, onClick = null, enabled = enabled)
                        Text(option.text, fontFamily = font, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        ExerciseType.sentence_select -> {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                e.options.forEach { option ->
                    WordTile(option.text, option.id in response.selected, enabled, font, "option-${option.id}") {
                        onChange(response.copy(selected = if (option.id in response.selected) response.selected - option.id else response.selected + option.id))
                    }
                }
            }
        }
        ExerciseType.sentence_bank, ExerciseType.sentence_order -> {
            Column(Modifier.fillMaxWidth().border(1.dp, Color(0xFFCFDAC8), RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TWOJA ODPOWIEDŹ", fontSize = 10.sp, color = Muted, letterSpacing = 1.sp)
                if (response.selected.isEmpty()) Text("Tutaj pojawią się wybrane słowa", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    response.selected.forEachIndexed { index, id ->
                        WordTile("${if (e.type == ExerciseType.sentence_bank) "${index + 1}. " else ""}${e.options.first { it.id == id }.text}", true, enabled, font) {
                            onChange(response.copy(selected = response.selected.toMutableList().also { it.removeAt(index) }))
                        }
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                session.optionOrder.forEach { id ->
                    val option = e.options.first { it.id == id }
                    WordTile(option.text, false, enabled && id !in response.selected && response.selected.size < e.correctOptionIds.size, font, "option-${option.id}") {
                        onChange(response.copy(selected = response.selected + id))
                    }
                }
            }
        }
    }
}

@Composable
private fun WordTile(text: String, selected: Boolean, enabled: Boolean, font: FontFamily, tag: String = "selected-tile", onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) Lime else Color.White, contentColor = Ink), modifier = Modifier.heightIn(min = 48.dp).testTag(tag), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Text(text, fontFamily = font, fontSize = 16.sp)
    }
}
