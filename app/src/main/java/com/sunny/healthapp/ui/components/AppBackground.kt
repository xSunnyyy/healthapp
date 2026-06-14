package com.sunny.healthapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.sunny.healthapp.ui.theme.Ink900
import com.sunny.healthapp.ui.theme.Ink950
import com.sunny.healthapp.ui.theme.ReadinessLilac
import com.sunny.healthapp.ui.theme.SleepBlue
import com.sunny.healthapp.ui.theme.WarmPeach

@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to Ink900,
                    0.6f to Ink950,
                    1.0f to Ink950,
                )
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Top-right lilac glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ReadinessLilac.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.02f),
                    radius = size.width * 0.85f,
                ),
                radius = size.width * 0.85f,
                center = Offset(size.width * 0.85f, size.height * 0.02f),
            )
            // Mid-left blue glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SleepBlue.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(-size.width * 0.1f, size.height * 0.45f),
                    radius = size.width * 0.7f,
                ),
                radius = size.width * 0.7f,
                center = Offset(-size.width * 0.1f, size.height * 0.45f),
            )
            // Bottom warm peach hint
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(WarmPeach.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.7f, size.height * 1.05f),
                    radius = size.width * 0.8f,
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width * 0.7f, size.height * 1.05f),
            )
        }
        content()
    }
}
