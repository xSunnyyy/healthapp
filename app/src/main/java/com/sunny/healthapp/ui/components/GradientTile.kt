package com.sunny.healthapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.ui.theme.Ink800
import com.sunny.healthapp.ui.theme.TextSecondary

/**
 * Top-row metric tile — soft light gradient sitting on the dark ink base.
 * Backlit pastel feel: bright at top-left fading toward transparent at bottom-right,
 * over a low-luminance base layer so the dark surface still reads through.
 */
@Composable
fun GradientTile(
    label: String,
    value: String,
    delta: String? = null,
    glowStart: Color,
    glowEnd: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Ink800)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        glowStart.copy(alpha = 0.55f),
                        glowStart.copy(alpha = 0.18f),
                        glowEnd.copy(alpha = 0.0f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                )
            )
            .border(
                width = 0.7.dp,
                brush = Brush.linearGradient(
                    0.0f to Color.White.copy(alpha = 0.30f),
                    1.0f to Color.White.copy(alpha = 0.02f),
                ),
                shape = RoundedCornerShape(22.dp),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.92f),
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                if (delta != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = delta,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

@Composable
fun GradientTileRow(
    tiles: List<@Composable (Modifier) -> Unit>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tiles.forEach { tile -> tile(Modifier.weight(1f).fillMaxSize()) }
    }
}
