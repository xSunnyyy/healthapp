package com.sunny.healthapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunny.healthapp.domain.model.BodyBatteryStatus
import com.sunny.healthapp.domain.model.BodyBatterySummary
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.MintGlow
import com.sunny.healthapp.ui.theme.Sunflare
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary

@Composable
fun BodyBatteryPanel(
    summary: BodyBatterySummary,
    modifier: Modifier = Modifier,
) {
    val color = when (summary.status) {
        BodyBatteryStatus.Charged -> MintGlow
        BodyBatteryStatus.Coasting -> Sunflare
        BodyBatteryStatus.Depleted -> Crimson
        BodyBatteryStatus.NoData -> TextMuted
    }
    val statusWord = when (summary.status) {
        BodyBatteryStatus.Charged -> "Charged"
        BodyBatteryStatus.Coasting -> "Coasting"
        BodyBatteryStatus.Depleted -> "Depleted"
        BodyBatteryStatus.NoData -> "Not enough data yet"
    }

    Panel(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.padding(end = 14.dp)) {
                Text(
                    text = "Body Battery",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = summary.current.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-2).sp,
                        ),
                        color = TextPrimary,
                    )
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
                    )
                }
                Text(
                    text = statusWord,
                    style = MaterialTheme.typography.titleSmall,
                    color = color,
                )
            }
            Spacer(Modifier.padding(end = 4.dp))
            if (summary.curve.size > 2) {
                BodyBatteryCurve(
                    summary = summary,
                    color = color,
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = subtitle(summary),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun BodyBatteryCurve(
    summary: BodyBatterySummary,
    color: Color,
    modifier: Modifier,
) {
    val anim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(900),
        label = "battery",
    )
    Canvas(modifier = modifier) {
        if (summary.curve.size < 2) return@Canvas

        val xs = summary.curve.map { it.minuteOfDay.toFloat() }
        val minX = xs.min()
        val maxX = xs.max()
        val rangeX = (maxX - minX).coerceAtLeast(1f)
        val padY = size.height * 0.12f

        // Soft grid line at 50% battery
        val midY = padY + (size.height - padY * 2) * 0.5f
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 0.7f,
        )

        val mapped = summary.curve.map {
            val nx = (it.minuteOfDay - minX) / rangeX
            val ny = 1f - it.value / 100f
            Offset(nx * size.width, padY + ny * (size.height - padY * 2))
        }

        // Smooth path
        val path = Path().apply {
            moveTo(mapped.first().x, mapped.first().y)
            for (i in 1 until mapped.size) {
                val p0 = mapped[i - 1]
                val p1 = mapped[i]
                val midX = (p0.x + p1.x) / 2f
                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }
        }
        // Fill under
        val fill = Path().apply {
            addPath(path)
            lineTo(mapped.last().x, size.height)
            lineTo(mapped.first().x, size.height)
            close()
        }
        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.32f), Color.Transparent),
            ),
        )
        // Animated stroke
        val animPath = Path().apply {
            moveTo(mapped.first().x, mapped.first().y)
            val cutoff = mapped.first().x + (mapped.last().x - mapped.first().x) * anim
            for (i in 1 until mapped.size) {
                val p0 = mapped[i - 1]
                val p1 = mapped[i]
                if (p1.x <= cutoff) {
                    val midX = (p0.x + p1.x) / 2f
                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                } else {
                    val t = ((cutoff - p0.x) / (p1.x - p0.x)).coerceIn(0f, 1f)
                    val ix = p0.x + (p1.x - p0.x) * t
                    val iy = p0.y + (p1.y - p0.y) * t
                    lineTo(ix, iy)
                    return@apply
                }
            }
        }
        drawPath(
            path = animPath,
            color = color,
            style = Stroke(width = 2.4f, cap = StrokeCap.Round),
        )
        // Endpoint dot
        val last = mapped.last()
        drawCircle(color = color, radius = 4f, center = last)
        drawCircle(color = Color.White, radius = 1.5f, center = last)
    }
}

private fun subtitle(summary: BodyBatterySummary): String {
    if (summary.status == BodyBatteryStatus.NoData) {
        return "Sleep a couple of nights with Fitbit and your battery curve will start filling in."
    }
    val delta = summary.current - summary.morningCharge
    val deltaText = when {
        delta == 0 -> "holding steady from this morning"
        delta > 0 -> "+$delta since this morning's $${summary.morningCharge}"
        else -> "$delta since this morning's $${summary.morningCharge}"
    }.replace("$$", "")
    return "Started the day at ${summary.morningCharge} · $deltaText."
}
