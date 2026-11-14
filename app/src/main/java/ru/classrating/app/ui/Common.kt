@file:OptIn(ExperimentalMaterial3Api::class)
package ru.classrating.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import ru.classrating.app.data.fmt

val DAY_SHORT = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
val DAY_FULL = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")

fun today(): Long = LocalDate.now().toEpochDay()
fun fmtDate(epochDay: Long, withYear: Boolean = false): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern(if (withYear) "d MMMM yyyy" else "d MMM", Locale("ru")))
fun fmtTime(min: Int) = "%02d:%02d".format(min / 60, min % 60)
fun parseTime(s: String): Int? {
    val p = s.trim().split(":")
    if (p.size != 2) return null
    val h = p[0].toIntOrNull() ?: return null
    val m = p[1].toIntOrNull() ?: return null
    return if (h in 0..23 && m in 0..59) h * 60 + m else null
}

fun gradeColor(v: Double): Color = when {
    v >= 4.5 -> Color(0xFF2E7D32)
    v >= 3.5 -> Color(0xFF1976D2)
    v >= 2.5 -> Color(0xFFF9A825)
    else -> Color(0xFFC62828)
}

@Composable
fun GradeChip(value: Int, size: Dp = 36.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(gradeColor(value.toDouble())),
        contentAlignment = Alignment.Center
    ) { Text(value.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.45f).sp) }
}

@Composable
fun AvgBadge(avg: Double?) {
    val color = avg?.let { gradeColor(it) } ?: MaterialTheme.colorScheme.outline
    Surface(shape = CircleShape, color = color.copy(alpha = 0.15f)) {
        Text(fmt(avg), Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

@Composable
fun EmptyHint(text: String) = Text(
    text, Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = TextAlign.Center,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val m = modifier.fillMaxWidth().animateContentSize()
    val colors = CardDefaults.elevatedCardColors()
    if (onClick != null) ElevatedCard(onClick, m, colors = colors) { Column(Modifier.padding(16.dp), content = content) }
    else ElevatedCard(m, colors = colors) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun ScreenScaffold(
    title: String, onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}, fab: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onBack != null) IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                },
                actions = actions
            )
        },
        floatingActionButton = fab, content = content
    )
}

@Composable
fun FormDialog(
    title: String, onDismiss: () -> Unit, confirmEnabled: Boolean = true,
    confirmText: String = "Сохранить", onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) },
        confirmButton = { TextButton(onConfirm, enabled = confirmEnabled) { Text(confirmText) } },
        dismissButton = { TextButton(onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun <T> DropdownField(label: String, items: List<T>, selected: T?, text: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(
            value = selected?.let(text) ?: "", onValueChange = {}, readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            items.forEach { DropdownMenuItem(text = { Text(text(it)) }, onClick = { onSelect(it); expanded = false }) }
        }
    }
}

@Composable
fun DateField(label: String, epochDay: Long, onChange: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = fmtDate(epochDay, true), onValueChange = {}, readOnly = true, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = { IconButton({ open = true }) { Icon(Icons.Filled.CalendarMonth, "Выбрать дату") } }
    )
    if (open) {
        val st = rememberDatePickerState(initialSelectedDateMillis = epochDay * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton({ st.selectedDateMillis?.let { onChange(Math.floorDiv(it, 86_400_000L)) }; open = false }) { Text("ОК") }
            },
            dismissButton = { TextButton({ open = false }) { Text("Отмена") } }
        ) { DatePicker(st) }
    }
}
