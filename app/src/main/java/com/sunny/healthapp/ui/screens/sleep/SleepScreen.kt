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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.domain.model.SleepStage
import com.sunny.healthapp.domain.model.SleepSummary
import com.sunny.healthapp.ui.components.BarChart7Day
import com.sunny.healthapp.ui.components.BarPoint
import com.sunny.healthapp.ui.components.EditorialHeader
import com.sunny.healthapp.ui.components.Panel
import com.sunny.healthapp.ui.components.Period
import com.sunny.healthapp.ui.components.PeriodTabs
import com.sunny.healthapp.ui.components.RefreshableContent
import com.sunny.healthapp.ui.components.SleepStageInfoSheet
import com.sunny.healthapp.ui.components.SleepStagesBar
import com.sunny.healthapp.ui.components.StaggeredEnter
import com.sunny.healthapp.ui.components.SyncDot
import com.sunny.healthapp.ui.screens.PermissionGate
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.AccentDeep
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.Lavender
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
        val sync by vm.syncStatus.collectAsStateWithLifecycle()
        if (state.loading && state.singleSession == null && state.aggregate == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent, strokeWidth = 2.dp)
            }
        } else {
            Content(state, sync, onSetPeriod = vm::setPeriod, onSync = vm::manualSync)
        }
    }
}

@Composable
private fun Content(
    state: SleepState,
    sync: com.sunny.healthapp.data.sync.SyncStatus,
    onSetPeriod: (Period) -> Unit,
    onSync: () -> Unit,
) {
    var stageToExplain by remember { mutableStateOf<com.sunny.healthapp.domain.model.SleepStage?>(null) }
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isRefreshing = sync is com.sunny.healthapp.data.sync.SyncStatus.Syncing
    stageToExplain?.let { stage ->
        SleepStageInfoSheet(stage = stage, onDismiss = { stageToExplain = null })
    }
    RefreshableContent(
        isRefreshing = isRefreshing,
        onRefresh = onSync,
        modifier = Modifier.fillMaxSize(),
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = statusInset + 12.dp, bottom = 130.dp),
    ) {
        StaggeredEnter(0) { m ->
            EditorialHeader(
                eyebrow = if (state.period == Period.D) "Sleep · last night" else "Sleep · ${periodLabel(state.period)}",
                title = if (state.period == Period.D) "How well\ndid you rest?" else "Your rest,\nover time.",
                modifier = m,
            )
        }
        Spacer(Modifier.height(14.dp))
        StaggeredEnter(1) { m ->
            Row(
                modifier = m.padding(horizontal = 20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PeriodTabs(
                    selected = state.period,
                    onSelect = onSetPeriod,
                    modifier = Modifier.weight(1f),
                )
                SyncDot(status = sync, onClick = onSync, size = 30.dp)
            }
        }
        Spacer(Modifier.height(18.dp))

        when (state.period) {
            Period.D -> DayContent(state.singleSession, onStageClick = { stageToExplain = it })
            else -> AggregateContent(state)
        }
    }
    }
}

@Composable
private fun DayContent(
    sleep: SleepSummary?,
    onStageClick: (com.sunny.healthapp.domain.model.SleepStage) -> Unit,
) {
    StaggeredEnter(2) { m ->
        Box(modifier = m.padding(horizontal = 20.dp)) {
            Panel(modifier = Modifier.fillMaxWidth()) {
                Text("Time asleep", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = sleep?.let { formatDuration(it.total.toMinutes()) } ?: "—",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                )
                if (sleep != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = sessionLabel(sleep),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    val awakeMin = sleep.awake.toMinutes()
                    if (awakeMin > 0) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${awakeMin}m awake · ${sleep.efficiencyPct}% efficiency",
                            style = MaterialTheme.typography.labelMedium,
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
                    SleepStagesBar(sleep = sleep, onStageClick = onStageClick)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        StaggeredEnter(4) { m ->
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

@Composable
private fun AggregateContent(state: SleepState) {
    val agg = state.aggregate
    val pLabel = periodLabel(state.period)
    StaggeredEnter(2) { m ->
        Box(modifier = m.padding(horizontal = 20.dp)) {
            Panel(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Avg time asleep", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = agg?.let { formatDuration(it.avgTotal.toMinutes()) } ?: "—",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                        )
                        Text(
                            text = "${agg?.sessionCount ?: 0} nights · $pLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Avg score", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = (agg?.avgScore ?: 0).toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Accent,
                        )
                        Text(
                            text = agg?.let { "${it.avgEfficiency}% eff." } ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))

    if (agg != null && agg.nightly.isNotEmpty()) {
        StaggeredEnter(3) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Nightly duration",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(14.dp))
                    val points = agg.nightly.map {
                        BarPoint(label = it.labelDay.toString(), value = it.totalMin.toFloat())
                    }
                    BarChart7Day(points = points, color = Accent)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        StaggeredEnter(4) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Avg stage breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(14.dp))
                    val totalMin = (agg.avgDeep.toMinutes() + agg.avgRem.toMinutes() +
                            agg.avgLight.toMinutes() + agg.avgAwake.toMinutes()).coerceAtLeast(1L)
                    AvgStageRow("DEEP", agg.avgDeep, totalMin, AccentDeep)
                    Spacer(Modifier.height(10.dp))
                    AvgStageRow("REM", agg.avgRem, totalMin, Lavender)
                    Spacer(Modifier.height(10.dp))
                    AvgStageRow("LIGHT", agg.avgLight, totalMin, Accent)
                    Spacer(Modifier.height(10.dp))
                    AvgStageRow("AWAKE", agg.avgAwake, totalMin, Crimson)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        StaggeredEnter(5) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Text("Avg night vitals · $pLabel", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(14.dp))
                    VitalRow("Avg heart rate", agg.avgHr?.let { "$it bpm" } ?: "—", Crimson)
                    VitalRow("Avg HRV", agg.avgHrv?.toInt()?.let { "$it ms" } ?: "—", Lavender)
                    VitalRow("Avg SpO₂", agg.avgSpo2?.toInt()?.let { "$it%" } ?: "—", Accent)
                    VitalRow("Avg respiration", agg.avgRespiration?.let { "%.1f rpm".format(it) } ?: "—", Accent)
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        ) {
            Panel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No sleep sessions recorded in this range yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
        }
    }
}

@Composable
private fun AvgStageRow(label: String, duration: Duration, totalMin: Long, accent: Color) {
    val pct = ((duration.toMinutes() * 100.0) / totalMin).toInt()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(formatDuration(duration.toMinutes()), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.width(12.dp))
        Text("$pct%", style = MaterialTheme.typography.labelMedium, color = TextMuted)
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
    return "In bed ${f.format(s.start.atZone(z))} → ${f.format(s.end.atZone(z))}"
}

private fun periodLabel(p: Period): String = when (p) {
    Period.D -> "today"
    Period.W -> "7 days"
    Period.M -> "30 days"
    Period.SixM -> "6 months"
    Period.Y -> "year"
}
