package com.sunny.healthapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Tight horizontal day-strip: M T W T F S S row of letters above day-number
 * circles. Selected day has a filled white circle; today is highlighted in
 * the accent color; days with activity get a subtle accent border.
 */
@Composable
fun WeekStrip(
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    activeDays: Set<LocalDate> = emptySet(),
    modifier: Modifier = Modifier,
) {
    val monday = selected.with(DayOfWeek.MONDAY)
    val days = (0..6).map { monday.plusDays(it.toLong()) }
    val today = LocalDate.now()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        days.forEach { date ->
            DayCell(
                date = date,
                isSelected = date == selected,
                isToday = date == today,
                isActive = date in activeDays,
                onClick = { onSelect(date) },
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val numberColor = when {
        isSelected -> Color.Black
        isToday -> Accent
        else -> TextSecondary
    }
    val letterColor = when {
        isSelected -> TextPrimary
        isToday -> Accent
        else -> TextMuted
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 6.dp),
    ) {
        Text(
            text = date.dayOfWeek.name.take(1),
            style = MaterialTheme.typography.labelSmall,
            color = letterColor,
        )
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .then(
                    when {
                        isSelected -> Modifier.background(Color.White.copy(alpha = 0.95f))
                        isActive -> Modifier.border(0.8.dp, Accent.copy(alpha = 0.55f), CircleShape)
                        else -> Modifier.border(0.6.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = numberColor,
            )
        }
    }
}
