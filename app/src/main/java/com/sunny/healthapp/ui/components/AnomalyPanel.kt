package com.sunny.healthapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.domain.model.AnomalyInsight
import com.sunny.healthapp.domain.model.AnomalySeverity
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.Sunflare
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary

@Composable
fun AnomalyPanel(
    insights: List<AnomalyInsight>,
    modifier: Modifier = Modifier,
) {
    if (insights.isEmpty()) return
    Panel(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "What's different",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Compared to your last 30 days",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(14.dp))
        insights.forEachIndexed { i, insight ->
            AnomalyRow(insight)
            if (i != insights.lastIndex) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color.White.copy(alpha = 0.05f)),
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun AnomalyRow(insight: AnomalyInsight) {
    val tint = when (insight.severity) {
        AnomalySeverity.High -> Crimson
        AnomalySeverity.Medium -> Sunflare
        AnomalySeverity.Low -> TextSecondary
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when (insight.severity) {
                    AnomalySeverity.High -> Icons.Outlined.Warning
                    else -> Icons.Outlined.Info
                },
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.padding(top = 2.dp)) {
            Text(
                insight.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                insight.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}
