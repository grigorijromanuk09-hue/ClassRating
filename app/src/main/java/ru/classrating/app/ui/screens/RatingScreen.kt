@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package ru.classrating.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.classrating.app.data.*
import ru.classrating.app.ui.*

private val MEDALS = listOf("🥇", "🥈", "🥉")

private fun parseAvg(s: String): Double? = s.replace(',', '.').toDoubleOrNull()?.takeIf { it in 1.0..5.0 }

@Composable
fun RatingScreen(vm: AppViewModel) {
    val diary by vm.diary.collectAsStateWithLifecycle()
    val mates by vm.classmates.collectAsStateWithLifecycle()
    val scores by vm.scores.collectAsStateWithLifecycle()
    val name by vm.prefs.studentName.collectAsStateWithLifecycle()
    var subjectId by remember { mutableStateOf<Long?>(null) }
    var addMate by remember { mutableStateOf(false) }
    var addScore by remember { mutableStateOf(false) }
    val rows = remember(diary, mates, scores, subjectId, name) { buildRating(name, diary, mates, scores, subjectId) }

    ScreenScaffold("Рейтинг класса",
        actions = { IconButton({ addScore = true }, enabled = mates.isNotEmpty() && diary.stats.isNotEmpty()) { Icon(Icons.Filled.Edit, "Оценка по предмету") } },
        fab = { ExtendedFloatingActionButton(onClick = { addMate = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Ученик") }) }
    ) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(subjectId == null, { subjectId = null }, label = { Text("Общий") })
                    diary.stats.forEach { st -> FilterChip(subjectId == st.subject.id, { subjectId = st.subject.id }, label = { Text(st.subject.name) }) }
                }
            }
            if (rows.isEmpty()) item {
                EmptyHint("Пока нечего сравнивать. Добавьте ученика кнопкой «Ученик» и укажите средний балл. Ваш балл считается из оценок дневника.")
            }
            if (rows.size >= 2) item {
                Text("Топ-3", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.take(3).forEachIndexed { i, r ->
                        ElevatedCard(Modifier.weight(1f), colors = CardDefaults.elevatedCardColors(
                            containerColor = if (r.isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(MEDALS[i], fontSize = 28.sp)
                                Text(r.name, maxLines = 2, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                                Text(fmt(r.avg), fontWeight = FontWeight.Bold, color = gradeColor(r.avg))
                            }
                        }
                    }
                }
            }
            itemsIndexed(rows, key = { _, r -> r.classmateId ?: -1L }) { i, r ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", Modifier.width(36.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                            color = if (i < 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (r.isMe) "${r.name} (вы)" else r.name, Modifier.weight(1f),
                            fontWeight = if (r.isMe) FontWeight.Bold else FontWeight.Normal)
                        AvgBadge(r.avg)
                        if (!r.isMe) IconButton({ mates.firstOrNull { it.id == r.classmateId }?.let(vm::deleteClassmate) }) {
                            Icon(Icons.Filled.Delete, "Удалить ученика")
                        }
                    }
                }
            }
        }
    }

    if (addMate) {
        var n by remember { mutableStateOf("") }
        var a by remember { mutableStateOf("") }
        FormDialog("Новый ученик", { addMate = false }, n.isNotBlank() && parseAvg(a) != null,
            onConfirm = { vm.addClassmate(n, parseAvg(a)!!); addMate = false }) {
            OutlinedTextField(n, { n = it }, label = { Text("Имя") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(a, { a = it }, label = { Text("Средний балл (1–5)") }, singleLine = true,
                isError = a.isNotEmpty() && parseAvg(a) == null, modifier = Modifier.fillMaxWidth())
        }
    }
    if (addScore) {
        var m by remember { mutableStateOf(mates.firstOrNull()) }
        var s by remember { mutableStateOf(diary.stats.firstOrNull()?.subject) }
        var a by remember { mutableStateOf("") }
        FormDialog("Балл по предмету", { addScore = false }, m != null && s != null && parseAvg(a) != null,
            onConfirm = { vm.setScore(m!!.id, s!!.id, parseAvg(a)!!); addScore = false }) {
            DropdownField("Ученик", mates, m, { it.name }) { m = it }
            DropdownField("Предмет", diary.stats.map { it.subject }, s, { it.name }) { s = it }
            OutlinedTextField(a, { a = it }, label = { Text("Средний балл (1–5)") }, singleLine = true,
                isError = a.isNotEmpty() && parseAvg(a) == null, modifier = Modifier.fillMaxWidth())
        }
    }
}
