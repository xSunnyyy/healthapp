package com.sunny.healthapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunny.healthapp.ui.theme.Ink850
import com.sunny.healthapp.ui.theme.TextSecondary

/**
 * Tall capsule stat tile. Pure glass surface with a faint top-anchored
 * accent bloom, fully rounded vertical pill shape so they stand out from
 * the rest of the editorial dark theme. Just label + value, no delta line.
 */
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
    @Suppress("UNUSED_PARAMETER") val _delta = delta
    @Suppress("UNUSED_PARAMETER") val _glowEnd = glowEnd
    val accent = glowStart
    val shape = RoundedCornerShape(48.dp)

    Box(
        modifier = modifier
            .clip(shape)
            // Layered glass: solid translucent base + subtle vertical lift
            .background(Ink850.copy(alpha = 0.55f))
            .background(
                Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.10f),
                    0.5f to Color.White.copy(alpha = 0.02f),
                    1.0f to Color.White.copy(alpha = 0.0f),
                )
            )
            // Accent bloom anchored at the top, fading by ~50% down
            .background(
                Brush.verticalGradient(
                    0.0f to accent.copy(alpha = 0.28f),
                    0.45f to Color.Transparent,
                    1.0f to Color.Transparent,
                )
            )
            // Soft accent halo at the bottom too — gives the capsule depth
            .background(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(Float.POSITIVE_INFINITY / 2f, Float.POSITIVE_INFINITY),
                    radius = 400f,
                )
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.32f),
                    0.5f to Color.White.copy(alpha = 0.08f),
                    1.0f to Color.White.copy(alpha = 0.02f),
                ),
                shape = shape,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        // Top: small-caps label + tiny accent ring stacked
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 22.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AccentDot(color = accent)
            androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 1.8.sp,
                ),
                color = TextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Center: BIG numeral
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1).sp,
            ),
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 6.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun AccentDot(color: Color) {
    Canvas(modifier = Modifier.size(8.dp)) {
        drawCircle(
            color = color.copy(alpha = 0.22f),
            radius = size.minDimension / 2f,
        )
        drawCircle(
            color = color,
            radius = size.minDimension / 3f,
        )
    }
}

@Composable
fun GradientTileRow(
    tiles: List<@Composable (Modifier) -> Unit>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(170.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tiles.forEach { tile -> tile(Modifier.weight(1f).height(170.dp)) }
    }
}
