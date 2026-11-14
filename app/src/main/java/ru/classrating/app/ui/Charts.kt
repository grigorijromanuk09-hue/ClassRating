package ru.classrating.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp

/** Линейный график (значения 1..5) с анимацией прорисовки. */
@Composable
fun TrendChart(values: List<Float>, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) { progress.snapTo(0f); progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val grid = MaterialTheme.colorScheme.outlineVariant
    if (values.size < 2) { EmptyHint("Недостаточно оценок для графика"); return }
    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        for (g in 2..5) {
            val y = size.height * (1f - (g - 1f) / 4f)
            drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height * (1f - (v.coerceIn(1f, 5f) - 1f) / 4f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        clipRect(right = size.width * progress.value) {
            drawPath(path, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Горизонтальная гистограмма: сколько пятёрок, четвёрок, троек и двоек. */
@Composable
fun DistributionChart(dist: Map<Int, Int>) {
    val max = (dist.values.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (v in 5 downTo 2) {
            val count = dist[v] ?: 0
            val frac by animateFloatAsState(count.toFloat() / max, tween(700), label = "bar")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$v", Modifier.width(20.dp), style = MaterialTheme.typography.labelLarge)
                Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(frac).clip(RoundedCornerShape(7.dp)).background(gradeColor(v.toDouble())))
                }
                Text("$count", Modifier.width(36.dp), style = MaterialTheme.typography.labelLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }
}
