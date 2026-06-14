package com.sunny.healthapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
 * Horizontal strip of the current week with day-letters above and day-numbers below.
 * Selected day = filled circle around the number. Today + days with activity get a
 * faint glow ring above the letter (used as the "fire" replacement in the reference).
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Text(
            text = date.dayOfWeek.name.take(1),
            style = MaterialTheme.typography.labelMedium,
            color = when {
                isSelected -> TextPrimary
                isToday -> Accent
                else -> TextSecondary
            },
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier.size(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Accent.copy(alpha = 0.7f))
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .then(
                    if (isSelected) {
                        Modifier
                            .background(Color.White.copy(alpha = 0.95f))
                    } else {
                        Modifier.border(0.7.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = if (isSelected) Color.Black else if (isToday) Accent else TextSecondary,
            )
        }
    }
}
