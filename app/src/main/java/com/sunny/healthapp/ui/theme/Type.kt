package com.sunny.healthapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Editorial type system, three families, no external dependencies:
// - Serif italic for editorial headlines (Noto Serif on Android)
// - Sans-serif thin/light for big numerals (refined data display)
// - Monospace small-caps for measurements, periods, ticks
val Editorial = FontFamily.Serif
val Numeric = FontFamily.SansSerif
val Mono = FontFamily.Monospace
val Body = FontFamily.SansSerif

val HealthTypography = Typography(
    // Massive editorial italic — the only "voice" in the app
    displayLarge = TextStyle(
        fontFamily = Editorial,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Editorial,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Editorial,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 22.sp,
        lineHeight = 26.sp,
    ),

    // Thin numerals for the big stat values
    headlineLarge = TextStyle(
        fontFamily = Numeric,
        fontWeight = FontWeight.Light,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-2).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Numeric,
        fontWeight = FontWeight.Light,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Numeric,
        fontWeight = FontWeight.Light,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
    ),

    titleLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    ),

    bodyLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),

    labelLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp,
    ),
    // Monospace small-caps for measurement labels: "BPM", "STEPS", "TODAY"
    labelMedium = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.6.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 2.0.sp,
    ),
)
