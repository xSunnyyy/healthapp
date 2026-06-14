package com.sunny.healthapp.ui.screens.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.domain.model.DailySummary
import com.sunny.healthapp.domain.model.ReadinessSummary
import com.sunny.healthapp.domain.model.SleepSummary
import com.sunny.healthapp.ui.components.ArcGauge
import com.sunny.healthapp.ui.components.ChipMetric
import com.sunny.healthapp.ui.components.GlassCard
import com.sunny.healthapp.ui.components.MetricChipStrip
import com.sunny.healthapp.ui.components.ScoreRing
import com.sunny.healthapp.ui.screens.PermissionGate
import com.sunny.healthapp.ui.theme.ActivityGreen
import com.sunny.healthapp.ui.theme.HeartRed
import com.sunny.healthapp.ui.theme.ReadinessLilac
import com.sunny.healthapp.ui.theme.SleepBlue
import com.sunny.healthapp.ui.theme.WarmPeach
import java.time.LocalDate
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
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
        }
        return
    }

    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = statusInset + 12.dp, bottom = 130.dp),
    ) {
        Greeting(modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(20.dp))
        MetricChipStrip(metrics = chips(state))
        Spacer(Modifier.height(28.dp))
        Hero(readiness = state.readiness)
        Spacer(Modifier.height(36.dp))
        ActivitySection(daily = state.daily)
        Spacer(Modifier.height(20.dp))
        SleepSection(sleep = state.sleep)
        Spacer(Modifier.height(20.dp))
        VitalsSection(daily = state.daily, sleep = state.sleep)
    }
}

@Composable
private fun Greeting(modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
    Column(modifier = modifier) {
        Text(
            text = "Good morning".uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = today.format(fmt),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private fun chips(state: HomeState): List<ChipMetric> {
    val sleep = state.sleep
    val daily = state.daily
    val readiness = state.readiness
    return listOf(
        ChipMetric(
            icon = Icons.Outlined.SelfImprovement,
            label = "Readiness",
            value = readiness?.score?.toString() ?: "—",
            accent = ReadinessLilac,
        ),
        ChipMetric(
            icon = Icons.Outlined.Bedtime,
            label = "Sleep",
            value = sleep?.let { shortDuration(it.total.toMinutes()) } ?: "—",
            accent = SleepBlue,
        ),
        ChipMetric(
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            label = "Steps",
            value = daily?.steps?.let { abbreviate(it) } ?: "—",
            accent = ActivityGreen,
        ),
        ChipMetric(
            icon = Icons.Outlined.MonitorHeart,
            label = "Heart",
            value = (daily?.latestHeartRate ?: daily?.avgHeartRate)?.toString() ?: "—",
            accent = HeartRed,
        ),
        ChipMetric(
            icon = Icons.Outlined.Bedtime,
            label = "Sleep score",
            value = sleep?.score?.toString() ?: "—",
            accent = WarmPeach,
        ),
    )
}

@Composable
private fun Hero(readiness: ReadinessSummary?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScoreRing(
            score = readiness?.score ?: 0,
            label = "Readiness",
            color = ReadinessLilac,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = verdict(readiness?.score ?: 0),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle(readiness?.score ?: 0),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun verdict(score: Int): String = when {
    score >= 85 -> "Primed."
    score >= 70 -> "Steady."
    score >= 55 -> "Take it easy."
    else -> "Recover."
}

private fun subtitle(score: Int): String = when {
    score >= 85 -> "Your body is ready for a strong day."
    score >= 70 -> "Solid baseline — pace yourself."
    score >= 55 -> "Light activity will serve you better today."
    else -> "Sleep, hydrate, and keep load low."
}

@Composable
private fun ActivitySection(daily: DailySummary?) {
    val steps = daily?.steps ?: 0L
    val cals = daily?.totalCalories ?: 0.0
    val move = daily?.exerciseMinutes ?: 0L

    GlassCard(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        tint = ActivityGreen,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Today".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArcGauge(
                progress = (steps / 10_000f),
                label = "Steps",
                value = compactSteps(steps),
                target = "of 10k",
                color = ActivityGreen,
            )
            ArcGauge(
                progress = (cals / 2_500.0).toFloat(),
                label = "Calories",
                value = "%,d".format(cals.toInt()),
                target = "of 2.5k",
                color = WarmPeach,
            )
            ArcGauge(
                progress = (move / 30f),
                label = "Move",
                value = move.toString(),
                target = "of 30 min",
                color = HeartRed,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MiniStat(
                label = "Distance",
                value = "%.2f".format((daily?.distanceMeters ?: 0.0) / 1000.0),
                unit = "km",
                accent = ActivityGreen,
                modifier = Modifier.weight(1f),
            )
            MiniStat(
                label = "Floors",
                value = (daily?.floorsClimbed ?: 0.0).toInt().toString(),
                unit = null,
                accent = WarmPeach,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SleepSection(sleep: SleepSummary?) {
    GlassCard(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        tint = SleepBlue,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Last night".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = sleep?.let { formatDuration(it.total.toMinutes()) } ?: "—",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(16.dp))
        if (sleep == null) {
            Text(
                text = "No sleep recorded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StagePill("Deep", formatDuration(sleep.deep.toMinutes()), SleepBlue, Modifier.weight(1f))
                StagePill("REM", formatDuration(sleep.rem.toMinutes()), ReadinessLilac, Modifier.weight(1f))
                StagePill("Light", formatDuration(sleep.light.toMinutes()), Color(0xFF8AB3FF), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniStat(
                    label = "Score",
                    value = sleep.score.toString(),
                    unit = null,
                    accent = SleepBlue,
                    modifier = Modifier.weight(1f),
                )
                MiniStat(
                    label = "Efficiency",
                    value = sleep.efficiencyPct.toString(),
                    unit = "%",
                    accent = SleepBlue,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun VitalsSection(daily: DailySummary?, sleep: SleepSummary?) {
    GlassCard(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        tint = HeartRed,
    ) {
        Text(
            text = "Vitals".uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MiniStat(
                label = "Heart",
                value = (daily?.latestHeartRate ?: daily?.avgHeartRate)?.toString() ?: "—",
                unit = "bpm",
                accent = HeartRed,
                modifier = Modifier.weight(1f),
            )
            MiniStat(
                label = "Resting",
                value = (daily?.restingHeartRate ?: sleep?.avgHeartRate)?.toString() ?: "—",
                unit = "bpm",
                accent = HeartRed,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MiniStat(
                label = "HRV",
                value = sleep?.avgHrv?.toInt()?.toString() ?: "—",
                unit = "ms",
                accent = ReadinessLilac,
                modifier = Modifier.weight(1f),
            )
            MiniStat(
                label = "SpO₂",
                value = sleep?.avgSpo2?.toInt()?.toString() ?: "—",
                unit = "%",
                accent = SleepBlue,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StagePill(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: String,
    unit: String?,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (unit != null) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

private fun formatDuration(totalMinutes: Long): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun shortDuration(totalMinutes: Long): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h == 0L -> "${m}m"
        m == 0L -> "${h}h"
        else -> "${h}h${m}"
    }
}

private fun compactSteps(n: Long): String = when {
    n >= 10_000 -> "%.1fk".format(n / 1000.0)
    n >= 1_000 -> "%,d".format(n)
    else -> n.toString()
}

private fun abbreviate(n: Long): String = compactSteps(n)
