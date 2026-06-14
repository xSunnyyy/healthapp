package com.sunny.healthapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.ui.theme.TextMuted

data class BarPoint(val label: String, val value: Float)

@Composable
fun BarChart7Day(
    points: List<BarPoint>,
    highlightIndex: Int? = null,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    showLabels: Boolean = true,
) {
    val max = (points.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f)
    val anim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 850),
        label = "bars",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(height),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            points.forEachIndexed { i, p ->
                val ratio = (p.value / max).coerceIn(0.04f, 1f)
                val isHi = highlightIndex == i
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(height * ratio * anim),
                    ) {
                        val grad = Brush.verticalGradient(
                            0.0f to (if (isHi) color else color.copy(alpha = 0.35f)),
                            1.0f to (if (isHi) color.copy(alpha = 0.65f) else color.copy(alpha = 0.18f)),
                        )
                        drawRoundRect(
                            brush = grad,
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(size.width / 2f, size.width / 2f),
                        )
                    }
                }
            }
        }
        if (showLabels) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                points.forEachIndexed { i, p ->
                    val isHi = highlightIndex == i
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = p.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isHi) color else TextMuted,
                        )
                    }
                }
            }
        }
    }
}

data class LinePoint(val x: Float, val y: Float)

@Composable
fun SmoothLineChart(
    points: List<LinePoint>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    fillBelow: Boolean = true,
    drawDot: Boolean = true,
) {
    val anim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 950),
        label = "line",
    )

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        if (points.size < 2) return@Canvas

        val xs = points.map { it.x }
        val ys = points.map { it.y }
        val minX = xs.min()
        val maxX = xs.max()
        val minY = ys.min()
        val maxY = ys.max()
        val rangeX = (maxX - minX).coerceAtLeast(1e-3f)
        val rangeY = (maxY - minY).coerceAtLeast(1e-3f)
        val padY = size.height * 0.12f

        // Grid lines (3 horizontals)
        repeat(3) { row ->
            val y = padY + (size.height - padY * 2) * (row / 2f)
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.7f,
            )
        }

        val mapped = points.map {
            val nx = (it.x - minX) / rangeX
            val ny = 1f - ((it.y - minY) / rangeY)
            Offset(nx * size.width, padY + ny * (size.height - padY * 2))
        }

        // Catmull-Rom-ish smooth path via cubic segments
        val path = Path().apply {
            moveTo(mapped.first().x, mapped.first().y)
            for (i in 1 until mapped.size) {
                val p0 = mapped[(i - 1).coerceAtLeast(0)]
                val p1 = mapped[i]
                val midX = (p0.x + p1.x) / 2f
                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }
        }

        // Fill under the curve
        if (fillBelow) {
            val fill = Path().apply {
                addPath(path)
                lineTo(mapped.last().x, size.height)
                lineTo(mapped.first().x, size.height)
                close()
            }
            drawPath(
                path = fill,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.30f), Color.Transparent),
                ),
            )
        }

        // Animated stroke — clip width by anim
        val animPath = Path().apply {
            moveTo(mapped.first().x, mapped.first().y)
            val cutoffX = mapped.first().x + (mapped.last().x - mapped.first().x) * anim
            for (i in 1 until mapped.size) {
                val p0 = mapped[i - 1]
                val p1 = mapped[i]
                if (p1.x <= cutoffX) {
                    val midX = (p0.x + p1.x) / 2f
                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                } else {
                    val t = ((cutoffX - p0.x) / (p1.x - p0.x)).coerceIn(0f, 1f)
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

        if (drawDot) {
            val last = mapped.last()
            val cutoff = mapped.first().x + (mapped.last().x - mapped.first().x) * anim
            if (last.x <= cutoff) {
                drawCircle(color = color, radius = 4f, center = last)
                drawCircle(color = Color.White, radius = 1.6f, center = last)
            }
        }
    }
}

@Composable
fun MiniSparkline(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
) {
    val points = values.mapIndexed { i, v -> LinePoint(i.toFloat(), v) }
    SmoothLineChart(
        points = points,
        color = color,
        modifier = modifier,
        height = height,
        fillBelow = false,
        drawDot = false,
    )
}
