package com.sunny.healthapp.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.domain.model.DailySummary
import com.sunny.healthapp.domain.model.ReadinessSummary
import com.sunny.healthapp.domain.model.SleepSummary
import com.sunny.healthapp.ui.components.ActivityRing
import com.sunny.healthapp.ui.components.ActivityRings
import com.sunny.healthapp.ui.components.MetricCard
import com.sunny.healthapp.ui.components.ScoreRing
import com.sunny.healthapp.ui.components.SectionHeader
import com.sunny.healthapp.ui.screens.PermissionGate
import com.sunny.healthapp.ui.theme.ActivityGreen
import com.sunny.healthapp.ui.theme.HeartRed
import com.sunny.healthapp.ui.theme.ReadinessLilac
import com.sunny.healthapp.ui.theme.SleepBlue
import com.sunny.healthapp.ui.theme.WarmPeach
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen() {
    PermissionGate {
        val app = LocalContext.current.applicationContext as HealthApp
        val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
        val state by vm.state.collectAsStateWithLifecycle()
        HomeContent(state)
    }
}

@Composable
private fun HomeContent(state: HomeState) {
    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Greeting()
        ReadinessHero(state.readiness)
        SleepHighlights(state.sleep)
        ActivityHighlights(state.daily)
        VitalsRow(state.daily, state.sleep)
    }
}

@Composable
private fun Greeting() {
    val today = java.time.LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
    Column {
        Text(
            "Good morning",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            today.format(fmt),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ReadinessHero(readiness: ReadinessSummary?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ScoreRing(
            score = readiness?.score ?: 0,
            label = "Readiness",
            color = ReadinessLilac,
        )
        Text(
            text = readinessVerdict(readiness?.score ?: 0),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

private fun readinessVerdict(score: Int): String = when {
    score >= 85 -> "You're primed for the day."
    score >= 70 -> "Solid baseline — pace yourself."
    score >= 55 -> "Take it easier today."
    else -> "Body is asking for recovery."
}

@Composable
private fun SleepHighlights(sleep: SleepSummary?) {
    Column {
        SectionHeader("Last night", subtitle = sleep?.let {
            val h = it.total.toMinutes() / 60
            val m = it.total.toMinutes() % 60
            "${h}h ${m}m asleep"
        } ?: "No sleep recorded")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                label = "Sleep score",
                value = (sleep?.score ?: 0).toString(),
                accent = SleepBlue,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Efficiency",
                value = (sleep?.efficiencyPct ?: 0).toString(),
                unit = "%",
                accent = SleepBlue,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ActivityHighlights(daily: DailySummary?) {
    Column {
        SectionHeader("Today's activity")
        Row(verticalAlignment = Alignment.CenterVertically) {
            val stepsProgress = ((daily?.steps ?: 0L) / 10_000f).coerceAtLeast(0f)
            val calProgress = ((daily?.activeCalories ?: 0.0) / 500.0).toFloat().coerceAtLeast(0f)
            val exerciseProgress = ((daily?.exerciseMinutes ?: 0L) / 30f).coerceAtLeast(0f)
            ActivityRings(
                rings = listOf(
                    ActivityRing(stepsProgress, ActivityGreen),
                    ActivityRing(calProgress, WarmPeach),
                    ActivityRing(exerciseProgress, HeartRed),
                ),
                size = 140.dp,
            )
            Column(
                modifier = Modifier.padding(start = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RingLegend("Steps", "${daily?.steps ?: 0}", "/ 10,000", ActivityGreen)
                RingLegend("Calories", "${(daily?.activeCalories ?: 0.0).toInt()}", "/ 500 kcal", WarmPeach)
                RingLegend("Move", "${daily?.exerciseMinutes ?: 0}", "/ 30 min", HeartRed)
            }
        }
    }
}

@Composable
private fun RingLegend(label: String, value: String, target: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Text("  $target", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun VitalsRow(daily: DailySummary?, sleep: SleepSummary?) {
    Column {
        SectionHeader("Vitals")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                label = "Resting HR",
                value = (daily?.restingHeartRate ?: sleep?.avgHeartRate ?: 0).toString(),
                unit = "bpm",
                accent = HeartRed,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "HRV",
                value = sleep?.avgHrv?.toInt()?.toString() ?: "—",
                unit = "ms",
                accent = ReadinessLilac,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            MetricCard(
                label = "SpO₂",
                value = sleep?.avgSpo2?.toInt()?.toString() ?: "—",
                unit = "%",
                accent = SleepBlue,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Respiration",
                value = sleep?.avgRespiratoryRate?.let { "%.1f".format(it) } ?: "—",
                unit = "rpm",
                accent = SleepBlue,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
