package com.sunny.healthapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val START_ANGLE = 135f
private const val SWEEP_TOTAL = 270f

@Composable
fun ArcGauge(
    progress: Float,
    label: String,
    value: String,
    target: String? = null,
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 124.dp,
    strokeWidth: Dp = 5.dp,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceAtLeast(0f).coerceAtMost(1.4f),
        animationSpec = tween(durationMillis = 900),
        label = "arc",
    )

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            // Track
            drawArc(
                color = Color.White.copy(alpha = 0.07f),
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_TOTAL,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Halo
            drawArc(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width / 2f,
                ),
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_TOTAL * animated.coerceAtMost(1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke * 5, cap = StrokeCap.Round),
            )
            // Progress
            drawArc(
                color = color,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_TOTAL * animated.coerceAtMost(1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp),
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            if (target != null) {
                Text(
                    text = target,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

