package com.sunny.healthapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ActivityRing(
    val progress: Float,
    val color: Color,
)

@Composable
fun ActivityRings(
    rings: List<ActivityRing>,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    strokeWidth: Dp = 14.dp,
    gap: Dp = 6.dp,
) {
    val animated = rings.map {
        animateFloatAsState(
            targetValue = it.progress.coerceIn(0f, 1.5f),
            animationSpec = tween(durationMillis = 900),
            label = "ring",
        )
    }

    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val gapPx = gap.toPx()
        rings.forEachIndexed { index, ring ->
            val inset = index * (stroke + gapPx)
            val arcSize = Size(this.size.width - stroke - inset * 2, this.size.height - stroke - inset * 2)
            val topLeft = Offset(stroke / 2f + inset, stroke / 2f + inset)
            drawArc(
                color = ring.color.copy(alpha = 0.18f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = ring.color,
                startAngle = -90f,
                sweepAngle = (360f * animated[index].value).coerceAtMost(360f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
