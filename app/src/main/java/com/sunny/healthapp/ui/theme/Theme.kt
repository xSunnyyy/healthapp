package com.sunny.healthapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val EditorialDark = darkColorScheme(
    primary = Accent,
    onPrimary = Ink950,
    secondary = Crimson,
    onSecondary = Ink950,
    tertiary = Lavender,
    onTertiary = Ink950,
    background = Ink950,
    onBackground = TextPrimary,
    surface = Ink800,
    onSurface = TextPrimary,
    surfaceVariant = Ink750,
    onSurfaceVariant = TextSecondary,
    outline = EdgeSoft,
    outlineVariant = EdgeFaint,
)

@Composable
fun HealthAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EditorialDark,
        typography = HealthTypography,
        content = content,
    )
}
