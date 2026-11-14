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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.classrating.app.ui.*

@Composable
fun HomeworkScreen(vm: AppViewModel) {
    val homework by vm.homework.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val names = remember(subjects) { subjects.associate { it.id to it.name } }
    var filter by remember { mutableIntStateOf(0) } // 0 активные, 1 выполненные, 2 все
    var add by remember { mutableStateOf(false) }
    var needSubject by remember { mutableStateOf(false) }
    val list = homework.filter { when (filter) { 0 -> !it.done; 1 -> it.done; else -> true } }

    ScreenScaffold("Домашние задания", fab = {
        ExtendedFloatingActionButton(onClick = { if (subjects.isEmpty()) needSubject = true else add = true },
            icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Задание") })
    }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Активные", "Выполненные", "Все").forEachIndexed { i, t -> FilterChip(filter == i, { filter = i }, label = { Text(t) }) }
                }
            }
            if (list.isEmpty()) item { EmptyHint(if (filter == 0) "Активных заданий нет" else "Здесь пока пусто") }
            items(list, key = { it.id }) { h ->
                val overdue = !h.done && h.dueEpochDay < today()
                AppCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Checkbox(h.done, { vm.toggleHomework(h) })
                        Column(Modifier.weight(1f).padding(top = 12.dp)) {
                            Text(names[h.subjectId] ?: "—", fontWeight = FontWeight.SemiBold)
                            Text(h.text, textDecoration = if (h.done) TextDecoration.LineThrough else null)
                            Text(
                                (if (h.done) "Выполнено" else if (overdue) "Просрочено · " else "Сдать · ") +
                                    (if (h.done) "" else fmtDate(h.dueEpochDay, true)),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton({ vm.deleteHomework(h) }) { Icon(Icons.Filled.Delete, "Удалить задание") }
                    }
                }
            }
        }
    }
    if (needSubject) AddSubjectDialog({ needSubject = false }) { vm.addSubject(it); needSubject = false }
    if (add) {
        var subject by remember { mutableStateOf(subjects.firstOrNull()) }
        var text by remember { mutableStateOf("") }
        var due by remember { mutableLongStateOf(today() + 1) }
        FormDialog("Новое задание", { add = false }, subject != null && text.isNotBlank(),
            onConfirm = { vm.addHomework(subject!!.id, text, due); add = false }) {
            DropdownField("Предмет", subjects, subject, { it.name }) { subject = it }
            OutlinedTextField(text, { text = it }, label = { Text("Описание задания") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            DateField("Дата выполнения", due) { due = it }
        }
    }
}
