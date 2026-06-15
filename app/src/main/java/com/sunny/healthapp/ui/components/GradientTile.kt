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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunny.healthapp.ui.theme.Ink850
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextSecondary

/**
 * Glassmorphic stat tile. Three layers from back to front:
 *   1. Dark glass surface (translucent Ink850 + subtle white lift)
 *   2. Soft accent-color bloom at the top, fading to transparent ~40%
 *   3. Hairline gradient border around the whole shape
 *
 * A tiny accent-tinted ring of light sits in the top-right corner as the
 * only colored identifier. Big numeral in the center, tiny mono small-caps
 * label at the top, delta micro-line at the bottom.
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
    val accent = glowStart
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Ink850.copy(alpha = 0.55f))
            // soft white lift gradient — the glass
            .background(
                Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.08f),
                    0.55f to Color.White.copy(alpha = 0.02f),
                    1.0f to Color.White.copy(alpha = 0.0f),
                )
            )
            // accent bloom anchored at the top, fading by ~40% down
            .background(
                Brush.verticalGradient(
                    0.0f to accent.copy(alpha = 0.18f),
                    0.45f to Color.Transparent,
                    1.0f to Color.Transparent,
                )
            )
            .border(
                width = 0.7.dp,
                brush = Brush.linearGradient(
                    0.0f to Color.White.copy(alpha = 0.32f),
                    0.5f to Color.White.copy(alpha = 0.06f),
                    1.0f to Color.White.copy(alpha = 0.02f),
                ),
                shape = shape,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        // Top-right accent ring (the only colored identifier)
        AccentRing(
            color = accent,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 14.dp),
        )

        // Label small-caps top-left
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 1.6.sp,
            ),
            color = TextSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 18.dp),
        )

        // Big value centered both axes
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
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

        // Delta micro-line at the bottom, centered
        Text(
            text = delta ?: " ",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextMuted,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp, start = 12.dp, end = 12.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun AccentRing(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(10.dp)) {
        // Outer faint halo
        drawCircle(
            color = color.copy(alpha = 0.20f),
            radius = size.minDimension / 2f,
        )
        // Solid inner dot
        drawCircle(
            color = color,
            radius = size.minDimension / 3.2f,
        )
    }
}

@Composable
fun GradientTileRow(
    tiles: List<@Composable (Modifier) -> Unit>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(146.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tiles.forEach { tile -> tile(Modifier.weight(1f).height(146.dp)) }
    }
}
