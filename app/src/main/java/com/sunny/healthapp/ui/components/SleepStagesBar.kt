package com.sunny.healthapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.domain.model.SleepSegment
import com.sunny.healthapp.domain.model.SleepStage
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
 * Four-row hypnogram: each stage gets its own horizontal track. Within a row,
 * time periods in that stage are rendered as rounded pill bars. Row labels sit
 * to the left of the chart so bars never get covered.
 */
@Composable
fun SleepStagesBar(
    segments: List<SleepSegment>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
) {
    val anim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 950),
        label = "stages",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        if (segments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No stage data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
        } else {
            val rows = listOf(SleepStage.Awake, SleepStage.REM, SleepStage.Light, SleepStage.Deep)
            val rowHeight = height / rows.size
            val originMs = segments.first().start.toEpochMilli()
            val endMs = segments.last().end.toEpochMilli()
            val totalMs = (endMs - originMs).coerceAtLeast(1L)

            Row(
                modifier = Modifier.fillMaxWidth().height(height),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.width(52.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    rows.forEach { stage ->
                        Box(
                            modifier = Modifier.fillMaxWidth().height(rowHeight),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = stage.name.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                        }
                    }
                }
                Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    rows.forEachIndexed { index, _ ->
                        val cy = rowHeight.toPx() * index + rowHeight.toPx() / 2f
                        drawLine(
                            color = Color.White.copy(alpha = 0.045f),
                            start = Offset(0f, cy),
                            end = Offset(size.width, cy),
                            strokeWidth = 1f,
                        )
                    }
                    val barH = rowHeight.toPx() * 0.45f
                    segments.forEach { seg ->
                        val rowIndex = rows.indexOf(seg.stage)
                        if (rowIndex < 0) return@forEach
                        val startX = ((seg.start.toEpochMilli() - originMs).toFloat() / totalMs) * size.width
                        val endX = ((seg.end.toEpochMilli() - originMs).toFloat() / totalMs) * size.width * anim
                        val drawEndX = endX.coerceAtLeast(startX + 2f).coerceAtMost(size.width)
                        if (drawEndX <= startX) return@forEach
                        val cy = rowHeight.toPx() * rowIndex + rowHeight.toPx() / 2f
                        drawRoundRect(
                            color = stageColor(seg.stage),
                            topLeft = Offset(startX, cy - barH / 2f),
                            size = Size(drawEndX - startX, barH),
                            cornerRadius = CornerRadius(barH / 2f, barH / 2f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LegendDot("Awake", Crimson)
            LegendDot("REM", Lavender)
            LegendDot("Light", Accent)
            LegendDot("Deep", AccentDeep)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}
