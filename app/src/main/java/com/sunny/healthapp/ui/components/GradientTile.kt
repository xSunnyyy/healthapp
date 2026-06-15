package com.sunny.healthapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunny.healthapp.ui.theme.Ink800

@Composable
fun GradientTile(
    label: String,
    value: String,
    delta: String? = null,
    glowStart: Color,
    glowEnd: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
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
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Label pinned to top, single line
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            // Value centered, auto-fit single line
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                textAlign = TextAlign.Center,
            )
            // Delta pinned to bottom
            Text(
                text = delta ?: " ",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun GradientTileRow(
    tiles: List<@Composable (Modifier) -> Unit>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(132.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tiles.forEach { tile -> tile(Modifier.weight(1f).height(132.dp)) }
    }
}
