package com.sunny.healthapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.domain.model.SleepSegment
import com.sunny.healthapp.domain.model.SleepStage
import com.sunny.healthapp.ui.theme.HeartRed
import com.sunny.healthapp.ui.theme.ReadinessLilac
import com.sunny.healthapp.ui.theme.SleepBlue
import com.sunny.healthapp.ui.theme.SleepBlueDeep

private fun stageColor(stage: SleepStage): Color = when (stage) {
    SleepStage.Awake -> HeartRed
    SleepStage.REM -> ReadinessLilac
    SleepStage.Light -> SleepBlue
    SleepStage.Deep -> SleepBlueDeep
    SleepStage.Unknown -> Color.Gray
}

private fun stageY(stage: SleepStage): Float = when (stage) {
    SleepStage.Awake -> 0.05f
    SleepStage.REM -> 0.30f
    SleepStage.Light -> 0.60f
    SleepStage.Deep -> 0.92f
    SleepStage.Unknown -> 0.60f
}

@Composable
fun SleepStagesBar(
    segments: List<SleepSegment>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            if (segments.isEmpty()) {
                Text(
                    text = "No sleep stage data",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    val totalNanos = (segments.last().end.toEpochMilli() -
                            segments.first().start.toEpochMilli()).coerceAtLeast(1)
                    val originMs = segments.first().start.toEpochMilli()
                    val barHeight = 14f
                    segments.forEach { seg ->
                        val startX = ((seg.start.toEpochMilli() - originMs).toFloat() / totalNanos) * size.width
                        val endX = ((seg.end.toEpochMilli() - originMs).toFloat() / totalNanos) * size.width
                        val centerY = size.height * stageY(seg.stage)
                        drawRect(
                            brush = SolidColor(stageColor(seg.stage)),
                            topLeft = Offset(startX, centerY - barHeight / 2f),
                            size = Size(endX - startX, barHeight),
                        )
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            LegendDot("Awake", HeartRed)
            LegendDot("REM", ReadinessLilac)
            LegendDot("Light", SleepBlue)
            LegendDot("Deep", SleepBlueDeep)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape),
        ) {
            Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(color = color)
            }
        }
        Text(
            text = "  $label",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
