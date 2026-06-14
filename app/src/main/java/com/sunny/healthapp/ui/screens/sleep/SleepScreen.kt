package com.sunny.healthapp.ui.screens.sleep

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.domain.model.SleepSummary
import com.sunny.healthapp.ui.components.EditorialHeader
import com.sunny.healthapp.ui.components.Panel
import com.sunny.healthapp.ui.components.Period
import com.sunny.healthapp.ui.components.PeriodTabs
import com.sunny.healthapp.ui.components.SleepStagesBar
import com.sunny.healthapp.ui.components.StaggeredEnter
import com.sunny.healthapp.ui.screens.PermissionGate
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.Lavender
import com.sunny.healthapp.ui.theme.LavenderDeep
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary
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
                CircularProgressIndicator(color = Accent, strokeWidth = 2.dp)
            }
        } else {
            Content(state.sleep)
        }
    }
}

@Composable
private fun Content(sleep: SleepSummary?) {
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var period by remember { mutableStateOf(Period.D) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = statusInset + 12.dp, bottom = 130.dp),
    ) {
        StaggeredEnter(0) { m ->
            EditorialHeader(
                eyebrow = "Sleep · last night",
                title = "How well\ndid you rest?",
                modifier = m,
            )
        }
        Spacer(Modifier.height(20.dp))
        StaggeredEnter(1) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                PeriodTabs(selected = period, onSelect = { period = it })
            }
        }
        Spacer(Modifier.height(18.dp))

        StaggeredEnter(2) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Time asleep", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = sleep?.let { formatDuration(it.total.toMinutes()) } ?: "—",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = TextPrimary,
                                )
                            }
                            if (sleep != null) {
                                Text(
                                    text = sessionLabel(sleep),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Score", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = (sleep?.score ?: 0).toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Accent,
                            )
                            Text(
                                text = sleep?.let { "${it.efficiencyPct}% eff." } ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (sleep != null) {
            StaggeredEnter(3) { m ->
                Box(modifier = m.padding(horizontal = 20.dp)) {
                    Panel(modifier = Modifier.fillMaxWidth()) {
                        Text("Stages", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(14.dp))
                        SleepStagesBar(segments = sleep.segments)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            StaggeredEnter(4) { m ->
                Row(
                    modifier = m.padding(horizontal = 20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StageStat("Deep", sleep.deep, LavenderDeep, Modifier.weight(1f))
                    StageStat("REM", sleep.rem, Lavender, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
            StaggeredEnter(5) { m ->
                Row(
                    modifier = m.padding(horizontal = 20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StageStat("Light", sleep.light, Accent, Modifier.weight(1f))
                    StageStat("Awake", sleep.awake, Crimson, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
            StaggeredEnter(6) { m ->
                Box(modifier = m.padding(horizontal = 20.dp)) {
                    Panel(modifier = Modifier.fillMaxWidth()) {
                        Text("Night Vitals", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(14.dp))
                        VitalRow("Avg heart rate", sleep.avgHeartRate?.let { "$it bpm" } ?: "—", Crimson)
                        VitalRow("HRV", sleep.avgHrv?.toInt()?.let { "$it ms" } ?: "—", Lavender)
                        VitalRow("SpO₂", sleep.avgSpo2?.toInt()?.let { "$it%" } ?: "—", Accent)
                        VitalRow("Respiration", sleep.avgRespiratoryRate?.let { "%.1f rpm".format(it) } ?: "—", Accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun StageStat(label: String, duration: Duration, accent: Color, modifier: Modifier = Modifier) {
    Panel(modifier = modifier, contentPadding = 16.dp) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent)
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatDuration(duration.toMinutes()),
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
        )
    }
}

@Composable
private fun VitalRow(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = accent)
    }
}

private fun formatDuration(totalMinutes: Long): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun sessionLabel(s: SleepSummary): String {
    val z = ZoneId.systemDefault()
    val f = DateTimeFormatter.ofPattern("h:mm a")
    return "${f.format(s.start.atZone(z))} — ${f.format(s.end.atZone(z))}"
}
