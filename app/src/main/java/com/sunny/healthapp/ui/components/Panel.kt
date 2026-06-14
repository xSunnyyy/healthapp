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
import com.sunny.healthapp.ui.theme.Ink800
import com.sunny.healthapp.ui.theme.Ink850

/**
 * The base raised dark card. Subtle vertical lift gradient + hairline edge.
 * Distinct from GradientTile (used for the bright top-row tiles).
 */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    contentPadding: Dp = 22.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    0.0f to Ink800,
                    1.0f to Ink850,
                )
            )
            .border(
                width = 0.6.dp,
                brush = Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.10f),
                    0.5f to Color.White.copy(alpha = 0.03f),
                    1.0f to Color.White.copy(alpha = 0.01f),
                ),
                shape = RoundedCornerShape(cornerRadius),
            ),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
