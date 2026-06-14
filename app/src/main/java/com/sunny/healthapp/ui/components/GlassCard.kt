package com.sunny.healthapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    contentPadding: Dp = 20.dp,
    tint: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    0.0f to Color.White.copy(alpha = 0.07f),
                    1.0f to Color.White.copy(alpha = 0.015f),
                )
            )
            .background(
                Brush.linearGradient(
                    0.0f to tint.copy(alpha = 0.12f),
                    1.0f to Color.Transparent,
                )
            )
            .border(
                width = 0.7.dp,
                brush = Brush.linearGradient(
                    0.0f to Color.White.copy(alpha = 0.28f),
                    0.5f to Color.White.copy(alpha = 0.08f),
                    1.0f to Color.White.copy(alpha = 0.02f),
                ),
                shape = RoundedCornerShape(cornerRadius),
            ),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
