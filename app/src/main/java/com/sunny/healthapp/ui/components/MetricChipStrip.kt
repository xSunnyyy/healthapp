package com.sunny.healthapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class ChipMetric(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val accent: Color,
)

@Composable
fun MetricChipStrip(
    metrics: List<ChipMetric>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        metrics.forEach { MetricChip(it) }
    }
}

@Composable
private fun MetricChip(m: ChipMetric) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .width(82.dp)
            .border(
                width = 0.7.dp,
                brush = Brush.linearGradient(
                    0.0f to Color.White.copy(alpha = 0.28f),
                    1.0f to Color.White.copy(alpha = 0.05f),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .background(
                Brush.linearGradient(
                    0.0f to Color.White.copy(alpha = 0.08f),
                    1.0f to Color.White.copy(alpha = 0.02f),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .background(
                Brush.linearGradient(
                    0.0f to m.accent.copy(alpha = 0.10f),
                    1.0f to Color.Transparent,
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = m.icon,
            contentDescription = m.label,
            tint = m.accent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = m.value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = m.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
