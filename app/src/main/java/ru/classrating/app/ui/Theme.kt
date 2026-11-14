package ru.classrating.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF4355B9), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF), onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF5A5D72), tertiary = Color(0xFF00696B),
    background = Color(0xFFFBF8FF), surface = Color(0xFFFBF8FF)
)
private val Dark = darkColorScheme(
    primary = Color(0xFFBAC3FF), onPrimary = Color(0xFF08218A),
    primaryContainer = Color(0xFF293CA0), onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFC3C5DD), tertiary = Color(0xFF4CDADC),
    background = Color(0xFF121318), surface = Color(0xFF121318)
)

@Composable
fun ClassRatingTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
