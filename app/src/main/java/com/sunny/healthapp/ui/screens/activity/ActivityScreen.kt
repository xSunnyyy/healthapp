package com.sunny.healthapp.ui.screens.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.domain.model.DailySummary
import com.sunny.healthapp.ui.components.ActivityRing
import com.sunny.healthapp.ui.components.ActivityRings
import com.sunny.healthapp.ui.components.MetricCard
import com.sunny.healthapp.ui.components.SectionHeader
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
                CircularProgressIndicator(color = ActivityGreen)
            }
        } else {
            Content(state)
        }
    }
}

@Composable
private fun Content(state: ActivityState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SectionHeader("Activity", subtitle = "Today and the past week")

        val daily = state.today
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            val stepsProgress = ((daily?.steps ?: 0L) / 10_000f).coerceAtLeast(0f)
            val calProgress = ((daily?.activeCalories ?: 0.0) / 500.0).toFloat().coerceAtLeast(0f)
            val exerciseProgress = ((daily?.exerciseMinutes ?: 0L) / 30f).coerceAtLeast(0f)
            ActivityRings(
                rings = listOf(
                    ActivityRing(stepsProgress, ActivityGreen),
                    ActivityRing(calProgress, WarmPeach),
                    ActivityRing(exerciseProgress, HeartRed),
                ),
                size = 180.dp,
            )
            Column(
                modifier = Modifier.padding(start = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Stat("Steps", "${daily?.steps ?: 0}", ActivityGreen)
                Stat("Active kcal", "${(daily?.activeCalories ?: 0.0).toInt()}", WarmPeach)
                Stat("Move min", "${daily?.exerciseMinutes ?: 0}", HeartRed)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                label = "Distance",
                value = "%.2f".format((state.today?.distanceMeters ?: 0.0) / 1000.0),
                unit = "km",
                accent = ActivityGreen,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Floors",
                value = (state.today?.floorsClimbed ?: 0.0).toInt().toString(),
                accent = WarmPeach,
                modifier = Modifier.weight(1f),
            )
        }

        SectionHeader("Last 7 days")
        WeeklyBars(state.recent)
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color) {
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun WeeklyBars(days: List<DailySummary>) {
    val max = (days.maxOfOrNull { it.steps } ?: 1L).coerceAtLeast(1L)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            days.reversed().forEach { day ->
                val ratio = (day.steps.toFloat() / max).coerceIn(0.05f, 1f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height((110 * ratio).dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                            drawRect(color = ActivityGreen)
                        }
                    }
                    Text(
                        text = day.date.format(DateTimeFormatter.ofPattern("EE", Locale.getDefault())).take(2),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}
