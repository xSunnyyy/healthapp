package com.sunny.healthapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.ui.theme.EdgeSoft
import com.sunny.healthapp.ui.theme.Ink800
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary

enum class Period(val label: String) { D("D"), W("W"), M("M"), SixM("6M"), Y("Y") }

@Composable
fun PeriodTabs(
    selected: Period,
    onSelect: (Period) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Ink800.copy(alpha = 0.7f))
            .border(0.6.dp, EdgeSoft, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Period.entries.forEach { period ->
            val isSelected = period == selected
            val bg by animateColorAsState(
                targetValue = if (isSelected) Color.White.copy(alpha = 0.92f) else Color.Transparent,
                animationSpec = tween(220),
                label = "tabBg",
            )
            val fg by animateColorAsState(
                targetValue = if (isSelected) Color.Black else TextSecondary,
                animationSpec = tween(220),
                label = "tabFg",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .clickable { onSelect(period) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = period.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = fg,
                )
            }
        }
    }
}
