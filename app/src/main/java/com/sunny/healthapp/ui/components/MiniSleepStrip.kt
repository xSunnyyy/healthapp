package com.sunny.healthapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.domain.model.SleepStage
import com.sunny.healthapp.domain.model.SleepSummary
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.AccentDeep
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.Lavender
import com.sunny.healthapp.ui.theme.TextMuted

private fun stageColor(stage: SleepStage): Color = when (stage) {
    SleepStage.Awake -> Crimson
    SleepStage.REM -> Lavender
    SleepStage.Light -> Accent
    SleepStage.Deep -> AccentDeep
    SleepStage.Unknown -> Color.Gray
}

/**
 * Compact horizontal strip showing last night's sleep stages as colored
 * segments proportional to their duration. Distinct from bars / sparkline.
 */
@Composable
fun MiniSleepStrip(
    sleep: SleepSummary?,
    modifier: Modifier = Modifier,
    height: Dp = 22.dp,
) {
    if (sleep == null || sleep.segments.isEmpty()) {
        Text(
            text = "No sleep data",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = modifier,
        )
        return
    }
    val segments = sleep.segments
    val originMs = segments.first().start.toEpochMilli()
    val endMs = segments.last().end.toEpochMilli()
    val totalMs = (endMs - originMs).coerceAtLeast(1L)

    val anim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800),
        label = "miniStrip",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(height / 2)),
    ) {
        segments.forEach { seg ->
            val startX = ((seg.start.toEpochMilli() - originMs).toFloat() / totalMs) * size.width
            val endX = ((seg.end.toEpochMilli() - originMs).toFloat() / totalMs) * size.width * anim
            val drawEndX = endX.coerceAtMost(size.width)
            if (drawEndX <= startX) return@forEach
            drawRect(
                color = stageColor(seg.stage),
                topLeft = Offset(startX, 0f),
                size = Size(drawEndX - startX, size.height),
            )
        }
    }
}
