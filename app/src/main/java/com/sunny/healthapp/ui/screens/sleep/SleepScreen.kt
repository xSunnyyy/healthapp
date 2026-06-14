package com.sunny.healthapp.ui.screens.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.domain.model.SleepSummary
import com.sunny.healthapp.ui.components.MetricCard
import com.sunny.healthapp.ui.components.ScoreRing
import com.sunny.healthapp.ui.components.SectionHeader
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
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            SleepContent(state.sleep)
        }
    }
}

@Composable
private fun SleepContent(sleep: SleepSummary?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SectionHeader("Sleep", subtitle = sleep?.let { sessionLabel(it) } ?: "No session yet")
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ScoreRing(
                score = sleep?.score ?: 0,
                label = "Sleep",
                color = SleepBlue,
            )
        }
        if (sleep != null) {
            SectionHeader("Stages")
            SleepStagesBar(segments = sleep.segments)
            StageGrid(sleep)
            SectionHeader("Night vitals")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Avg HR", (sleep.avgHeartRate ?: 0).toString(), "bpm", HeartRed,
                    modifier = Modifier.weight(1f))
                MetricCard("HRV", sleep.avgHrv?.toInt()?.toString() ?: "—", "ms", ReadinessLilac,
                    modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("SpO₂", sleep.avgSpo2?.toInt()?.toString() ?: "—", "%", SleepBlue,
                    modifier = Modifier.weight(1f))
                MetricCard("Respiration",
                    sleep.avgRespiratoryRate?.let { "%.1f".format(it) } ?: "—", "rpm", SleepBlue,
                    modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StageGrid(sleep: SleepSummary) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StageCard("Deep", sleep.deep, SleepBlueDeep, Modifier.weight(1f))
        StageCard("REM", sleep.rem, ReadinessLilac, Modifier.weight(1f))
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 12.dp),
    ) {
        StageCard("Light", sleep.light, SleepBlue, Modifier.weight(1f))
        StageCard("Awake", sleep.awake, HeartRed, Modifier.weight(1f))
    }
}

@Composable
private fun StageCard(label: String, duration: Duration, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val h = duration.toMinutes() / 60
    val m = duration.toMinutes() % 60
    MetricCard(
        label = label,
        value = if (h > 0) "${h}h ${m}m" else "${m}m",
        accent = color,
        modifier = modifier,
    )
}

private fun sessionLabel(s: SleepSummary): String {
    val zone = ZoneId.systemDefault()
    val fmt = DateTimeFormatter.ofPattern("h:mm a")
    val start = fmt.format(s.start.atZone(zone))
    val end = fmt.format(s.end.atZone(zone))
    return "$start — $end"
}
