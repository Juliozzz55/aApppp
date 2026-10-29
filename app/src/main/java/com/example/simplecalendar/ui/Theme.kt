package com.example.simplecalendar.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F7CAC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8F5),
    onPrimaryContainer = Color(0xFF1F3A57),
    secondaryContainer = Color(0xFFE8EEF5),
    onSecondaryContainer = Color(0xFF1F3A57),
    background = Color.White,
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF3F6FA),
    onSurfaceVariant = Color(0xFF5F6B7A),
    outline = Color(0xFFC5CED9),
    error = Color(0xFFB3261E)
)

@Composable
fun SimpleCalendarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
