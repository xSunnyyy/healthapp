package com.sunny.healthapp.ui.screens.sleep

import androidx.compose.foundation.background
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
import com.sunny.healthapp.domain.model.SleepSummary
import com.sunny.healthapp.ui.components.GlassCard
import com.sunny.healthapp.ui.components.ScoreRing
import com.sunny.healthapp.ui.components.SleepStagesBar
import com.sunny.healthapp.ui.screens.PermissionGate
import com.sunny.healthapp.ui.theme.HeartRed
import com.sunny.healthapp.ui.theme.ReadinessLilac
import com.sunny.healthapp.ui.theme.SleepBlue
import com.sunny.healthapp.ui.theme.SleepBlueDeep
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SleepScreen() {
    PermissionGate {
        val app = LocalContext.current.applicationContext as HealthApp
        val vm: SleepViewModel = viewModel(factory = SleepViewModel.factory(app))
        val state by vm.state.collectAsStateWithLifecycle()
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SleepBlue, strokeWidth = 2.dp)
            }
        } else {
            Content(state.sleep)
        }
    }
}

@Composable
private fun Content(sleep: SleepSummary?) {
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = statusInset + 16.dp, bottom = 130.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "Sleep".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = sleep?.let { sessionLabel(it) } ?: "No session",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(28.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScoreRing(score = sleep?.score ?: 0, label = "Sleep score", color = SleepBlue)
            Spacer(Modifier.height(16.dp))
            Text(
                text = sleep?.let { formatDuration(it.total.toMinutes()) } ?: "—",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = sleep?.let { "${it.efficiencyPct}% efficiency" } ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(32.dp))

        if (sleep != null) {
            GlassCard(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                tint = SleepBlue,
            ) {
                Text(
                    text = "Stages".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                SleepStagesBar(segments = sleep.segments)
            }
            Spacer(Modifier.height(16.dp))
            StageGrid(sleep)
            Spacer(Modifier.height(16.dp))
            NightVitals(sleep)
        }
    }
}

@Composable
private fun StageGrid(sleep: SleepSummary) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StageTile("Deep", sleep.deep, SleepBlueDeep, Modifier.weight(1f))
        StageTile("REM", sleep.rem, ReadinessLilac, Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StageTile("Light", sleep.light, SleepBlue, Modifier.weight(1f))
        StageTile("Awake", sleep.awake, HeartRed, Modifier.weight(1f))
    }
}

@Composable
private fun StageTile(label: String, duration: Duration, accent: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, tint = accent, contentPadding = 16.dp) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent)
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatDuration(duration.toMinutes()),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun NightVitals(sleep: SleepSummary) {
    GlassCard(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        tint = HeartRed,
    ) {
        Text(
            text = "Night vitals".uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        VitalRow("Avg heart rate", sleep.avgHeartRate?.let { "$it bpm" } ?: "—", HeartRed)
        VitalRow("HRV", sleep.avgHrv?.toInt()?.let { "$it ms" } ?: "—", ReadinessLilac)
        VitalRow("SpO₂", sleep.avgSpo2?.toInt()?.let { "$it%" } ?: "—", SleepBlue)
        VitalRow("Respiration", sleep.avgRespiratoryRate?.let { "%.1f rpm".format(it) } ?: "—", SleepBlue, isLast = true)
    }
}

@Composable
private fun VitalRow(label: String, value: String, accent: Color, isLast: Boolean = false) {
    Row(
        modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = accent,
        )
    }
    if (!isLast) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.07f)),
        )
    }
}

private fun formatDuration(totalMinutes: Long): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun sessionLabel(s: SleepSummary): String {
    val zone = ZoneId.systemDefault()
    val fmt = DateTimeFormatter.ofPattern("h:mm a")
    return "${fmt.format(s.start.atZone(zone))} — ${fmt.format(s.end.atZone(zone))}"
}
