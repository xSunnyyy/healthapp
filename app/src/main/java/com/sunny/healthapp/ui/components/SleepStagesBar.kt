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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun stageColor(stage: SleepStage): Color = when (stage) {
    SleepStage.Awake -> Crimson
    SleepStage.REM -> Lavender
    SleepStage.Light -> Accent
    SleepStage.Deep -> AccentDeep
    SleepStage.Unknown -> Color.Gray
}

/**
 * Sleep stages as a single horizontal "ribbon" of color spanning the night,
 * with time-of-night labels above and a tidy proportion list below. Reads at
 * a glance: dark blue = deep, blue = light, lavender = REM, red = awake.
 */
@Composable
fun SleepStagesBar(
    segments: List<SleepSegment>,
    modifier: Modifier = Modifier,
    ribbonHeight: Dp = 28.dp,
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
                    .height(60.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No stage data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
            return@Column
        }

        val originMs = segments.first().start.toEpochMilli()
        val endMs = segments.last().end.toEpochMilli()
        val totalMs = (endMs - originMs).coerceAtLeast(1L)
        val zone = ZoneId.systemDefault()
        val timeFmt = DateTimeFormatter.ofPattern("h:mm a")

        // Time axis
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = timeFmt.format(segments.first().start.atZone(zone)),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = timeFmt.format(segments.last().end.atZone(zone)),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(10.dp))

        // The ribbon itself
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ribbonHeight)
                .clip(RoundedCornerShape(ribbonHeight / 2)),
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(ribbonHeight)) {
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

        Spacer(Modifier.height(20.dp))

        // Stage breakdown list
        val byStage = segments.groupBy { it.stage }
            .mapValues { (_, list) -> list.fold(Duration.ZERO) { a, s -> a + s.duration } }
        val total = byStage.values.fold(Duration.ZERO) { a, b -> a + b }
        listOf(SleepStage.Deep, SleepStage.REM, SleepStage.Light, SleepStage.Awake).forEach { stage ->
            val d = byStage[stage] ?: Duration.ZERO
            val pct = if (total.isZero) 0
                else ((d.toMinutes().toDouble() / total.toMinutes()) * 100).toInt()
            StageRow(label = stage.name, duration = d, percent = pct, color = stageColor(stage))
            if (stage != SleepStage.Awake) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StageRow(label: String, duration: Duration, percent: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatDur(duration),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
        )
    }
}

private fun formatDur(d: Duration): String {
    val h = d.toMinutes() / 60
    val m = d.toMinutes() % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
