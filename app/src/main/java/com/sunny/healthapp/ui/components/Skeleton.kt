package com.sunny.healthapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animated diagonal shimmer overlay. Apply to any Box you want to look like a
 * loading placeholder.
 */
@Composable
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerPhase",
    )
    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.03f),
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.03f),
            ),
            start = Offset(phase * 600f - 300f, 0f),
            end = Offset(phase * 600f + 300f, 200f),
        ),
    )
}

/** Single shimmering block — primitive for skeleton compositions. */
@Composable
fun SkeletonBlock(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    corner: Dp = 6.dp,
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(corner))
            .shimmer(),
    )
}

@Composable
fun SkeletonLine(
    height: Dp = 12.dp,
    modifier: Modifier = Modifier,
    corner: Dp = 4.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(corner))
            .shimmer(),
    )
}

/** Stand-in for a Panel while data is loading. */
@Composable
fun SkeletonPanel(
    height: Dp = 140.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(20.dp),
    ) {
        Column {
            SkeletonLine(height = 14.dp, modifier = Modifier.width(140.dp))
            Spacer(Modifier.height(16.dp))
            SkeletonLine(height = 30.dp, modifier = Modifier.width(180.dp))
            Spacer(Modifier.height(12.dp))
            SkeletonLine(height = 10.dp, modifier = Modifier.fillMaxWidth())
        }
    }
}
