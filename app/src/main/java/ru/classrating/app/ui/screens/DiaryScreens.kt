@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package ru.classrating.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import ru.classrating.app.data.Subject
import ru.classrating.app.data.SubjectStat
import ru.classrating.app.data.fmt
import ru.classrating.app.ui.*

@Composable
fun DiaryScreen(vm: AppViewModel, nav: NavHostController) {
    val diary by vm.diary.collectAsStateWithLifecycle()
    var addGrade by remember { mutableStateOf(false) }
    var addSubject by remember { mutableStateOf(false) }

    ScreenScaffold("Электронный дневник",
        actions = { IconButton({ addSubject = true }) { Icon(Icons.Filled.PlaylistAdd, "Добавить предмет") } },
        fab = {
            ExtendedFloatingActionButton(onClick = {
                if (diary.stats.isEmpty()) addSubject = true else addGrade = true
            }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Оценка") })
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Общий средний балл", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        AvgBadge(diary.overall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("История успеваемости", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    TrendChart(diary.trend)
                }
            }
            if (diary.stats.isEmpty()) item { EmptyHint("Предметов пока нет. Нажмите «Оценка», чтобы добавить первый предмет") }
            items(diary.stats, key = { it.subject.id }) { st -> SubjectCard(st) { nav.navigate("subject/${st.subject.id}") } }
        }
    }

    if (addSubject) AddSubjectDialog({ addSubject = false }) { vm.addSubject(it); addSubject = false }
    if (addGrade) AddGradeDialog(diary.stats.map { it.subject }, null, { addGrade = false }) { sid, v, d, q, qn, c ->
        vm.addGrade(sid, v, d, q, qn, c); addGrade = false
    }
}

@Composable
private fun SubjectCard(st: SubjectStat, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(st.subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Оценок: ${st.current.size}", style = MaterialTheme.typography.bodySmall)
            }
            AvgBadge(st.avg)
        }
        if (st.current.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                st.current.take(8).forEach { GradeChip(it.value, 30.dp) }
            }
        }
        val quarters = (1..4).mapNotNull { q -> st.quarter(q)?.let { q to it } }
        if (quarters.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quarters.forEach { (q, v) -> AssistChip({}, label = { Text("$q чет.: $v") }) }
            }
        }
    }
}

@Composable
fun SubjectScreen(vm: AppViewModel, id: Long, onBack: () -> Unit) {
    val diary by vm.diary.collectAsStateWithLifecycle()
    val st = diary.stats.firstOrNull { it.subject.id == id }
    var addGrade by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    ScreenScaffold(st?.subject?.name ?: "Предмет", onBack = onBack,
        actions = { IconButton({ confirmDelete = true }) { Icon(Icons.Filled.Delete, "Удалить предмет") } },
        fab = { ExtendedFloatingActionButton(onClick = { addGrade = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Оценка") }) }
    ) { pad ->
        if (st == null) { Box(Modifier.padding(pad).fillMaxSize(), Alignment.Center) { Text("Предмет не найден") }; return@ScreenScaffold }
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Средний балл", Modifier.weight(1f), fontWeight = FontWeight.SemiBold); AvgBadge(st.avg)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Оценки за четверть", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        (1..4).forEach { q ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$q чет.", style = MaterialTheme.typography.bodySmall)
                                val v = st.quarter(q)
                                if (v != null) GradeChip(v) else Text("—", Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("История", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    TrendChart(st.current.sortedBy { it.epochDay }.map { it.value.toFloat() })
                }
            }
            item { SectionTitle("Все оценки") }
            if (st.grades.isEmpty()) item { EmptyHint("Оценок пока нет") }
            items(st.grades, key = { it.id }) { g ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GradeChip(g.value)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(fmtDate(g.epochDay, true), fontWeight = FontWeight.Medium)
                            val tag = if (g.isQuarter) "Четвертная, ${g.quarter} четверть" else g.comment
                            if (tag.isNotBlank()) Text(tag, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton({ vm.deleteGrade(g) }) { Icon(Icons.Filled.Delete, "Удалить оценку") }
                    }
                }
            }
        }
    }
    if (addGrade && st != null) AddGradeDialog(listOf(st.subject), st.subject, { addGrade = false }) { sid, v, d, q, qn, c ->
        vm.addGrade(sid, v, d, q, qn, c); addGrade = false
    }
    if (confirmDelete && st != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Удалить предмет?") },
        text = { Text("Будут удалены все оценки, уроки и задания по предмету «${st.subject.name}».") },
        confirmButton = { TextButton({ vm.deleteSubject(st.subject); confirmDelete = false; onBack() }) { Text("Удалить") } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Отмена") } }
    )
}

@Composable
fun AddSubjectDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    FormDialog("Новый предмет", onDismiss, name.isNotBlank(), onConfirm = { onSave(name) }) {
        OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddGradeDialog(
    subjects: List<Subject>, preselected: Subject?, onDismiss: () -> Unit,
    onSave: (subjectId: Long, value: Int, day: Long, isQuarter: Boolean, quarter: Int, comment: String) -> Unit
) {
    var subject by remember { mutableStateOf(preselected ?: subjects.firstOrNull()) }
    var value by remember { mutableIntStateOf(5) }
    var day by remember { mutableLongStateOf(today()) }
    var isQuarter by remember { mutableStateOf(false) }
    var quarter by remember { mutableIntStateOf(1) }
    var comment by remember { mutableStateOf("") }
    FormDialog("Новая оценка", onDismiss, subject != null, onConfirm = { onSave(subject!!.id, value, day, isQuarter, quarter, comment) }) {
        DropdownField("Предмет", subjects, subject, { it.name }) { subject = it }
        Text("Оценка", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (5 downTo 2).forEach { v -> FilterChip(value == v, { value = v }, label = { Text("$v") }) }
        }
        DateField("Дата", day) { day = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Четвертная", Modifier.weight(1f)); Switch(isQuarter, { isQuarter = it })
        }
        if (isQuarter) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..4).forEach { q -> FilterChip(quarter == q, { quarter = q }, label = { Text("$q чет.") }) }
        } else OutlinedTextField(comment, { comment = it }, label = { Text("Комментарий (необязательно)") }, modifier = Modifier.fillMaxWidth())
    }
}
