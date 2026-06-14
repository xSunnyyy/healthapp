package com.sunny.healthapp.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.domain.model.DailySummary
import com.sunny.healthapp.domain.model.ReadinessSummary
import com.sunny.healthapp.domain.model.SleepSummary
import com.sunny.healthapp.ui.components.ActivityRing
import com.sunny.healthapp.ui.components.ActivityRings
import com.sunny.healthapp.ui.components.ChipMetric
import com.sunny.healthapp.ui.components.HairLine
import com.sunny.healthapp.ui.components.ListRow
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
    ) {
        Greeting(modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(18.dp))
        MetricChipStrip(metrics = chips(state))
        Spacer(Modifier.height(28.dp))
        Hero(readiness = state.readiness)
        Spacer(Modifier.height(36.dp))

        SectionBlock(
            label = "Last night",
            value = state.sleep?.let { formatDuration(it.total.toMinutes()) } ?: "—",
        ) {
            SleepList(state.sleep)
        }

        Spacer(Modifier.height(24.dp))

        SectionBlock(
            label = "Today",
            value = state.daily?.steps?.toString() ?: "—",
            valueSuffix = "steps",
        ) {
            ActivityList(state.daily)
        }

        Spacer(Modifier.height(24.dp))

        SectionBlock(label = "Vitals", value = null) {
            VitalsList(state.daily, state.sleep)
        }

        Spacer(Modifier.height(40.dp))
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
            value = sleep?.let { formatShortDuration(it.total.toMinutes()) } ?: "—",
            accent = SleepBlue,
        ),
        ChipMetric(
            icon = Icons.Outlined.DirectionsRun,
            label = "Activity",
            value = daily?.steps?.let { abbreviate(it) } ?: "—",
            accent = ActivityGreen,
        ),
        ChipMetric(
            icon = Icons.Outlined.MonitorHeart,
            label = "Heart rate",
            value = (daily?.latestHeartRate ?: daily?.avgHeartRate)?.toString() ?: "—",
            accent = HeartRed,
        ),
        ChipMetric(
            icon = Icons.Outlined.Favorite,
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
            text = readinessVerdict(readiness?.score ?: 0),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = readinessSubtitle(readiness?.score ?: 0),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun readinessVerdict(score: Int): String = when {
    score >= 85 -> "Primed."
    score >= 70 -> "Steady."
    score >= 55 -> "Take it easy."
    else -> "Recover."
}

private fun readinessSubtitle(score: Int): String = when {
    score >= 85 -> "Your body is ready for a strong day."
    score >= 70 -> "Solid baseline — pace yourself."
    score >= 55 -> "Light activity will serve you better today."
    else -> "Sleep, hydrate, and keep load low."
}

@Composable
private fun SectionBlock(
    label: String,
    value: String?,
    valueSuffix: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (valueSuffix != null) {
                    Text(
                        text = "  $valueSuffix",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        HairLine()
        content()
    }
}

@Composable
private fun SleepList(sleep: SleepSummary?) {
    if (sleep == null) {
        Text(
            text = "No sleep recorded.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        return
    }
    ListRow("Sleep score", sleep.score.toString(), SleepBlue)
    HairLine()
    ListRow("Efficiency", "${sleep.efficiencyPct}%", SleepBlue)
    HairLine()
    ListRow("Deep", formatDuration(sleep.deep.toMinutes()), SleepBlue)
    HairLine()
    ListRow("REM", formatDuration(sleep.rem.toMinutes()), ReadinessLilac)
}

@Composable
private fun ActivityList(daily: DailySummary?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val steps = daily?.steps ?: 0L
        val totalCals = daily?.totalCalories ?: 0.0
        val move = daily?.exerciseMinutes ?: 0L
        ActivityRings(
            rings = listOf(
                ActivityRing((steps / 10_000f).coerceAtLeast(0f), ActivityGreen),
                ActivityRing((totalCals / 2_500.0).toFloat().coerceAtLeast(0f), WarmPeach),
                ActivityRing((move / 30f).coerceAtLeast(0f), HeartRed),
            ),
            size = 140.dp,
        )
        Column(
            modifier = Modifier.padding(start = 24.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatLine("Steps", steps.toString(), "/ 10,000", ActivityGreen)
            StatLine("Cal burned", "%,d".format(totalCals.toInt()), "kcal", WarmPeach)
            StatLine("Move", move.toString(), "/ 30 min", HeartRed)
        }
    }
    HairLine()
    ListRow(
        "Distance",
        "%.2f km".format((daily?.distanceMeters ?: 0.0) / 1000.0),
        ActivityGreen,
    )
    HairLine()
    ListRow(
        "Floors",
        (daily?.floorsClimbed ?: 0.0).toInt().toString(),
        WarmPeach,
    )
}

@Composable
private fun StatLine(label: String, value: String, suffix: String, accent: androidx.compose.ui.graphics.Color) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "  $suffix",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun VitalsList(daily: DailySummary?, sleep: SleepSummary?) {
    val currentHr = daily?.latestHeartRate ?: daily?.avgHeartRate
    ListRow(
        "Heart rate",
        currentHr?.let { "$it bpm" } ?: "—",
        HeartRed,
    )
    HairLine()
    ListRow(
        "Resting HR",
        (daily?.restingHeartRate ?: sleep?.avgHeartRate)?.let { "$it bpm" } ?: "—",
        HeartRed,
    )
    HairLine()
    ListRow(
        "HRV",
        sleep?.avgHrv?.toInt()?.let { "$it ms" } ?: "—",
        ReadinessLilac,
    )
    HairLine()
    ListRow(
        "SpO₂",
        sleep?.avgSpo2?.toInt()?.let { "$it%" } ?: "—",
        SleepBlue,
    )
    HairLine()
    ListRow(
        "Respiration",
        sleep?.avgRespiratoryRate?.let { "%.1f rpm".format(it) } ?: "—",
        SleepBlue,
    )
}

private fun formatDuration(totalMinutes: Long): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun formatShortDuration(totalMinutes: Long): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h == 0L -> "${m}m"
        m == 0L -> "${h}h"
        else -> "${h}h${m}"
    }
}

private fun abbreviate(n: Long): String = when {
    n >= 10_000 -> "%.1fk".format(n / 1000.0)
    n >= 1_000 -> "%,d".format(n)
    else -> n.toString()
}
