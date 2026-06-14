package com.sunny.healthapp.ui.screens.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.domain.model.DailySummary
import com.sunny.healthapp.ui.components.ArcGauge
import com.sunny.healthapp.ui.components.GlassCard
import com.sunny.healthapp.ui.screens.PermissionGate
import com.sunny.healthapp.ui.theme.ActivityGreen
import com.sunny.healthapp.ui.theme.HeartRed
import com.sunny.healthapp.ui.theme.WarmPeach
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ActivityScreen() {
    PermissionGate {
        val app = LocalContext.current.applicationContext as HealthApp
        val vm: ActivityViewModel = viewModel(factory = ActivityViewModel.factory(app))
        val state by vm.state.collectAsStateWithLifecycle()
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ActivityGreen, strokeWidth = 2.dp)
            }
        } else {
            Content(state)
        }
    }
}

@Composable
private fun Content(state: ActivityState) {
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val daily = state.today
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = statusInset + 16.dp, bottom = 130.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                "Activity".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Today and the past week",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(24.dp))
        GlassCard(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            tint = ActivityGreen,
        ) {
            Text(
                "Goals".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArcGauge(
                    progress = ((daily?.steps ?: 0L) / 10_000f),
                    label = "Steps",
                    value = compact(daily?.steps ?: 0L),
                    target = "of 10k",
                    color = ActivityGreen,
                    diameter = 132.dp,
                )
                ArcGauge(
                    progress = ((daily?.totalCalories ?: 0.0) / 2_500.0).toFloat(),
                    label = "Calories",
                    value = "%,d".format((daily?.totalCalories ?: 0.0).toInt()),
                    target = "of 2.5k",
                    color = WarmPeach,
                    diameter = 132.dp,
                )
                ArcGauge(
                    progress = ((daily?.exerciseMinutes ?: 0L) / 30f),
                    label = "Move",
                    value = (daily?.exerciseMinutes ?: 0L).toString(),
                    target = "of 30 min",
                    color = HeartRed,
                    diameter = 132.dp,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                "Distance",
                "%.2f".format((daily?.distanceMeters ?: 0.0) / 1000.0),
                "km",
                ActivityGreen,
                Modifier.weight(1f),
            )
            StatTile(
                "Floors",
                (daily?.floorsClimbed ?: 0.0).toInt().toString(),
                null,
                WarmPeach,
                Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(20.dp))
        GlassCard(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            tint = ActivityGreen,
        ) {
            Text(
                "Last 7 days".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            WeeklyBars(state.recent)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, unit: String?, accent: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, tint = accent, contentPadding = 16.dp) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            if (unit != null) {
                Text(" $unit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun WeeklyBars(days: List<DailySummary>) {
    val max = (days.maxOfOrNull { it.steps } ?: 1L).coerceAtLeast(1L)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        days.reversed().forEach { day ->
            val ratio = (day.steps.toFloat() / max).coerceIn(0.04f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height((110 * ratio).dp)
                        .clip(RoundedCornerShape(11.dp)),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color = ActivityGreen,
                            cornerRadius = CornerRadius(size.width / 2f, size.width / 2f),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = day.date.format(DateTimeFormatter.ofPattern("EE", Locale.getDefault())).take(2),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun compact(n: Long): String = when {
    n >= 10_000 -> "%.1fk".format(n / 1000.0)
    n >= 1_000 -> "%,d".format(n)
    else -> n.toString()
}
