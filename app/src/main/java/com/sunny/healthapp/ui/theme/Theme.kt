package com.sunny.healthapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HealthDark = darkColorScheme(
    primary = WarmPeach,
    onPrimary = Ink900,
    secondary = SleepBlue,
    onSecondary = Ink900,
    tertiary = ReadinessLilac,
    onTertiary = Ink900,
    background = Ink950,
    onBackground = TextPrimary,
    surface = Ink800,
    onSurface = TextPrimary,
    surfaceVariant = Ink700,
    onSurfaceVariant = TextSecondary,
    outline = GlassEdgeLight,
    outlineVariant = GlassEdgeDark,
)

@Composable
fun HealthAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HealthDark,
        typography = HealthTypography,
        content = content,
    )
}
