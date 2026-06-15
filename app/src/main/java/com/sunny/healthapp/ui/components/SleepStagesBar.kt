package com.sunny.healthapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
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
import com.sunny.healthapp.domain.model.SleepStage
import com.sunny.healthapp.domain.model.SleepSummary
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
 * Four independent stage tracks (AWAKE / REM / LIGHT / DEEP) so each stage's
 * timing is legible on its own. Row label on the left, time axis at the top,
 * and a tidy breakdown list below.
 */
@Composable
fun SleepStagesBar(
    sleep: SleepSummary,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 28.dp,
    onStageClick: ((SleepStage) -> Unit)? = null,
) {
    val segments = sleep.segments
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
        val rows = listOf(SleepStage.Awake, SleepStage.REM, SleepStage.Light, SleepStage.Deep)
        val labelWidth = 56.dp

        // Time axis with start/end labels aligned to the chart edges
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(labelWidth))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
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
            }
        }
        Spacer(Modifier.height(12.dp))

        // Per-stage tracks
        rows.forEachIndexed { index, stage ->
            Row(
                modifier = Modifier.fillMaxWidth().height(trackHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stage.name.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = stageColor(stage),
                    modifier = Modifier.width(labelWidth),
                )
                Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    // Faint baseline
                    val cy = size.height / 2f
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(0f, cy),
                        end = Offset(size.width, cy),
                        strokeWidth = 1f,
                    )
                    // Bars for this stage only
                    val barH = (size.height * 0.55f)
                    segments.filter { it.stage == stage }.forEach { seg ->
                        val startX = ((seg.start.toEpochMilli() - originMs).toFloat() / totalMs) * size.width
                        val endX = ((seg.end.toEpochMilli() - originMs).toFloat() / totalMs) * size.width * anim
                        val drawEndX = endX.coerceAtMost(size.width)
                        if (drawEndX <= startX) return@forEach
                        val width = (drawEndX - startX).coerceAtLeast(3f)
                        drawRoundRect(
                            color = stageColor(stage),
                            topLeft = Offset(startX, cy - barH / 2f),
                            size = Size(width, barH),
                            cornerRadius = CornerRadius(barH / 2f, barH / 2f),
                        )
                    }
                }
            }
            if (index != rows.lastIndex) Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.height(20.dp))

        // Breakdown list sourced from SleepSummary so all numbers agree.
        val durationsByStage = linkedMapOf(
            SleepStage.Deep to sleep.deep,
            SleepStage.REM to sleep.rem,
            SleepStage.Light to sleep.light,
            SleepStage.Awake to sleep.awake,
        )
        val totalMin = durationsByStage.values.sumOf { it.toMinutes() }.coerceAtLeast(1L)
        durationsByStage.forEach { (stage, dur) ->
            val pct = ((dur.toMinutes() * 100.0) / totalMin).toInt()
            StageRow(
                stage = stage,
                duration = dur,
                percent = pct,
                color = stageColor(stage),
                onClick = onStageClick?.let { cb -> { cb(stage) } },
            )
            if (stage != SleepStage.Awake) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StageRow(
    stage: SleepStage,
    duration: Duration,
    percent: Int,
    color: Color,
    onClick: (() -> Unit)? = null,
) {
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = clickMod.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.size(12.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stage.name.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            if (onClick != null) {
                Spacer(Modifier.size(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "About ${stage.name}",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
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
