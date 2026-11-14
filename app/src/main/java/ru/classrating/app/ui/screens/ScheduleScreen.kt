@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package ru.classrating.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.classrating.app.data.Lesson
import ru.classrating.app.data.Subject
import ru.classrating.app.data.defaultTimes
import ru.classrating.app.ui.*
import java.time.LocalDate

@Composable
fun ScheduleScreen(vm: AppViewModel) {
    val lessons by vm.lessons.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val names = remember(subjects) { subjects.associate { it.id to it.name } }
    var day by remember { mutableIntStateOf(LocalDate.now().dayOfWeek.value.let { if (it == 7) 1 else it }) }
    var add by remember { mutableStateOf(false) }
    var needSubject by remember { mutableStateOf(false) }

    ScreenScaffold("Расписание", fab = {
        ExtendedFloatingActionButton(onClick = { if (subjects.isEmpty()) needSubject = true else add = true },
            icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Урок") })
    }) { pad ->
        Column(Modifier.padding(pad)) {
            ScrollableTabRow(day - 1, edgePadding = 8.dp) {
                (0..5).forEach { i -> Tab(day - 1 == i, { day = i + 1 }, text = { Text(DAY_SHORT[i]) }) }
            }
            val list = lessons.filter { it.dayOfWeek == day }
            LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text(DAY_FULL[day - 1], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                if (list.isEmpty()) item { EmptyHint("В этот день уроков нет") }
                items(list, key = { it.id }) { l ->
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text("${l.number}", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(names[l.subjectId] ?: "—", fontWeight = FontWeight.SemiBold)
                                Text("${fmtTime(l.startMin)} – ${fmtTime(l.endMin)}", style = MaterialTheme.typography.bodySmall)
                                val extra = listOf(l.room.takeIf { it.isNotBlank() }?.let { "каб. $it" }, l.teacher.takeIf { it.isNotBlank() })
                                    .filterNotNull().joinToString(" · ")
                                if (extra.isNotBlank()) Text(extra, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton({ vm.deleteLesson(l) }) { Icon(Icons.Filled.Delete, "Удалить урок") }
                        }
                    }
                }
            }
        }
    }
    if (needSubject) AddSubjectDialog({ needSubject = false }) { vm.addSubject(it); needSubject = false }
    if (add) AddLessonDialog(subjects, day, { add = false }) { vm.addLesson(it); add = false }
}

@Composable
private fun AddLessonDialog(subjects: List<Subject>, initialDay: Int, onDismiss: () -> Unit, onSave: (Lesson) -> Unit) {
    var subject by remember { mutableStateOf(subjects.firstOrNull()) }
    var day by remember { mutableIntStateOf(initialDay) }
    var number by remember { mutableIntStateOf(1) }
    var start by remember { mutableStateOf(fmtTime(defaultTimes(1).first)) }
    var end by remember { mutableStateOf(fmtTime(defaultTimes(1).second)) }
    var room by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    val s = parseTime(start); val e = parseTime(end)
    val valid = subject != null && s != null && e != null && e > s
    FormDialog("Новый урок", onDismiss, valid, onConfirm = {
        onSave(Lesson(dayOfWeek = day, number = number, subjectId = subject!!.id, startMin = s!!, endMin = e!!,
            room = room.trim(), teacher = teacher.trim()))
    }) {
        DropdownField("Предмет", subjects, subject, { it.name }) { subject = it }
        DropdownField("День недели", (1..7).toList(), day, { DAY_FULL[it - 1] }) { day = it }
        DropdownField("Номер урока", (1..10).toList(), number, { "$it урок" }) {
            number = it; val t = defaultTimes(it); start = fmtTime(t.first); end = fmtTime(t.second)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(start, { start = it }, label = { Text("Начало") }, singleLine = true, isError = s == null, modifier = Modifier.weight(1f))
            OutlinedTextField(end, { end = it }, label = { Text("Конец") }, singleLine = true, isError = e == null || (s != null && e <= s), modifier = Modifier.weight(1f))
        }
        OutlinedTextField(room, { room = it }, label = { Text("Кабинет") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(teacher, { teacher = it }, label = { Text("Преподаватель") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Если в этот день уже есть урок с таким номером, он будет заменён.", style = MaterialTheme.typography.bodySmall)
    }
}
