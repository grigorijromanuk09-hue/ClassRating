@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package ru.classrating.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import ru.classrating.app.data.Lesson
import ru.classrating.app.data.fmt
import ru.classrating.app.ui.*
import java.time.LocalDateTime
import java.util.Locale

/** Ближайшие уроки: остаток сегодняшнего дня либо ближайший день с уроками. */
fun upcomingLessons(lessons: List<Lesson>): Pair<String, List<Lesson>> {
    val now = LocalDateTime.now()
    val dow = now.dayOfWeek.value
    val mins = now.hour * 60 + now.minute
    val rest = lessons.filter { it.dayOfWeek == dow && it.endMin > mins }
    if (rest.isNotEmpty()) return "Сегодня" to rest
    for (i in 1..7) {
        val d = (dow - 1 + i) % 7 + 1
        val l = lessons.filter { it.dayOfWeek == d }
        if (l.isNotEmpty()) return (if (i == 1) "Завтра" else DAY_FULL[d - 1]) to l
    }
    return "" to emptyList()
}

@Composable
fun HomeScreen(vm: AppViewModel, nav: NavHostController) {
    val name by vm.prefs.studentName.collectAsStateWithLifecycle()
    val cls by vm.prefs.className.collectAsStateWithLifecycle()
    val diary by vm.diary.collectAsStateWithLifecycle()
    val lessons by vm.lessons.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val homework by vm.homework.collectAsStateWithLifecycle()
    val names = remember(subjects) { subjects.associate { it.id to it.name } }
    val (dayLabel, next) = remember(lessons) { upcomingLessons(lessons) }
    val hw = remember(homework) { homework.filter { !it.done }.take(3) }
    val avg by animateFloatAsState((diary.overall ?: 0.0).toFloat(), tween(900), label = "avg")

    ScreenScaffold("Class Rating", actions = {
        IconButton({ nav.navigate("settings") }) { Icon(Icons.Filled.Settings, "Настройки") }
    }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Class Rating", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(name.ifBlank { "Ученик" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(if (cls.isBlank()) "Класс не указан" else "$cls класс", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (diary.overall == null) "—" else String.format(Locale("ru"), "%.2f", avg),
                                fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("средний балл", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            item {
                SectionTitle("Последние оценки")
                if (diary.recent.isEmpty()) EmptyHint("Оценок пока нет. Добавьте их во вкладке «Дневник»")
                else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    diary.recent.forEach { (g, s) ->
                        ElevatedCard {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GradeChip(g.value)
                                Column { Text(s.name, fontWeight = FontWeight.Medium, maxLines = 1); Text(fmtDate(g.epochDay), style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
            item {
                SectionTitle(if (dayLabel.isBlank()) "Ближайшие уроки" else "Ближайшие уроки · $dayLabel")
                if (next.isEmpty()) EmptyHint("Расписание не заполнено")
                else AppCard { next.take(4).forEach { l ->
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${fmtTime(l.startMin)}", Modifier.width(56.dp), fontWeight = FontWeight.SemiBold)
                        Column(Modifier.weight(1f)) {
                            Text(names[l.subjectId] ?: "—", fontWeight = FontWeight.Medium)
                            if (l.room.isNotBlank()) Text("каб. ${l.room}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } }
            }
            item {
                SectionTitle("Домашние задания")
                if (hw.isEmpty()) EmptyHint("Невыполненных заданий нет 🎉")
                else AppCard(onClick = { nav.navigate("homework") }) { hw.forEach { h ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(names[h.subjectId] ?: "—", fontWeight = FontWeight.Medium)
                        Text(h.text, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                        Text("до ${fmtDate(h.dueEpochDay)}", style = MaterialTheme.typography.bodySmall,
                            color = if (h.dueEpochDay < today()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } }
            }
            item {
                SectionTitle("Статистика успеваемости")
                AppCard {
                    Text("Распределение оценок", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    DistributionChart(diary.distribution)
                    Spacer(Modifier.height(16.dp))
                    Text("Динамика среднего балла", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    TrendChart(diary.trend)
                    val best = diary.stats.filter { it.avg != null }.maxByOrNull { it.avg!! }
                    val worst = diary.stats.filter { it.avg != null }.minByOrNull { it.avg!! }
                    if (best != null && worst != null && best != worst) {
                        Spacer(Modifier.height(8.dp))
                        Text("Лучший предмет: ${best.subject.name} (${fmt(best.avg)})", style = MaterialTheme.typography.bodySmall)
                        Text("Нужно подтянуть: ${worst.subject.name} (${fmt(worst.avg)})", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
