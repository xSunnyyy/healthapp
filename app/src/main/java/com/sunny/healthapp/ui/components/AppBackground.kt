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
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.Ink900
import com.sunny.healthapp.ui.theme.Ink950
import com.sunny.healthapp.ui.theme.Lavender

@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to Ink900,
                    0.35f to Ink950,
                    1.0f to Ink950,
                )
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Single discreet wash at the top to lift the headline
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Accent.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.5f, -size.height * 0.05f),
                    radius = size.width * 0.9f,
                ),
                radius = size.width * 0.9f,
                center = Offset(size.width * 0.5f, -size.height * 0.05f),
            )
            // Very faint bottom-left lavender bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Lavender.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(-size.width * 0.1f, size.height),
                    radius = size.width * 0.7f,
                ),
                radius = size.width * 0.7f,
                center = Offset(-size.width * 0.1f, size.height),
            )
        }
        content()
    }
}
