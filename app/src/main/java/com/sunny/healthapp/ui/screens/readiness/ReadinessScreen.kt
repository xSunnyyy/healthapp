package com.sunny.healthapp.ui.screens.readiness

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.ui.components.EditorialHeader
import com.sunny.healthapp.ui.components.LinePoint
import com.sunny.healthapp.ui.components.Panel
import com.sunny.healthapp.ui.components.Period
import com.sunny.healthapp.ui.components.PeriodTabs
import com.sunny.healthapp.ui.components.SmoothLineChart
import com.sunny.healthapp.ui.components.StaggeredEnter
import com.sunny.healthapp.ui.components.SyncIndicator
import com.sunny.healthapp.ui.components.ZoneBar
import com.sunny.healthapp.ui.screens.PermissionGate
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.MintGlow
import com.sunny.healthapp.ui.theme.Sunflare
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReadinessScreen() {
    PermissionGate {
        val app = LocalContext.current.applicationContext as HealthApp
        val vm: ReadinessViewModel = viewModel(factory = ReadinessViewModel.factory(app))
        val state by vm.state.collectAsStateWithLifecycle()
        val sync by vm.syncStatus.collectAsStateWithLifecycle()
        if (state.loading && state.latestHr == null) {
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
    state: ReadinessState,
    sync: com.sunny.healthapp.data.sync.SyncStatus,
    onSetPeriod: (Period) -> Unit,
    onSync: () -> Unit,
) {
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = statusInset + 12.dp, bottom = 130.dp),
    ) {
        StaggeredEnter(0) { m ->
            EditorialHeader(
                eyebrow = "Heart rate · readiness",
                title = "Today's heart\nin one glance",
                modifier = m,
            )
        }
        Spacer(Modifier.height(14.dp))
        StaggeredEnter(1) { m ->
            Row(
                modifier = m.padding(horizontal = 20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                SyncIndicator(status = sync, onClick = onSync)
            }
        }
        Spacer(Modifier.height(14.dp))

        StaggeredEnter(2) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                PeriodTabs(selected = state.period, onSelect = onSetPeriod)
            }
        }
        Spacer(Modifier.height(18.dp))

        StaggeredEnter(3) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Heart Rate", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = state.latestHr?.toString() ?: "—",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = TextPrimary,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "BPM",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 10.dp),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = state.latestHrTime?.let { friendlyTimestamp(it) } ?: "No samples",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Avg ${periodLabel(state.period)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(state.avgHr?.toString() ?: "—", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                                Spacer(Modifier.width(4.dp))
                                Text("BPM", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            val avg = state.avgHr ?: 0
                            val badgeLabel = when {
                                avg == 0 -> null
                                avg < 100 -> null
                                avg < 130 -> "Elevated" to Sunflare
                                else -> "High" to Crimson
                            }
                            badgeLabel?.let { (label, color) -> StatusBadge(label, color) }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    if (state.chartValues.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No heart-rate samples for this period yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                            )
                        }
                    } else {
                        val points = state.chartValues.mapIndexed { i, v -> LinePoint(i.toFloat(), v) }
                        SmoothLineChart(points = points, color = Crimson, height = 130.dp)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            state.chartLabels.forEach {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        StaggeredEnter(4) { m ->
            Row(
                modifier = m.padding(horizontal = 20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniStat("Average", state.avgHr?.toString() ?: "—", periodLabel(state.period), Modifier.weight(1f))
                MiniStat("Resting", state.restingHr?.toString() ?: "—", "today", Modifier.weight(1f))
                MiniStat(
                    "Max",
                    state.maxHr?.toString() ?: "—",
                    state.maxHrTime?.let { friendlyTime(it) } ?: periodLabel(state.period),
                    Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(18.dp))

        StaggeredEnter(5) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Text("Heart Rate Zones", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    ZoneBar("Resting", "Below 60 bpm", state.pctResting, Accent)
                    Spacer(Modifier.height(14.dp))
                    ZoneBar("Normal", "60–100 bpm", state.pctNormal, MintGlow)
                    Spacer(Modifier.height(14.dp))
                    ZoneBar("Elevated", "100–130 bpm", state.pctElevated, Sunflare)
                    Spacer(Modifier.height(14.dp))
                    ZoneBar("High", "130+ bpm", state.pctHigh, Crimson)
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Panel(modifier = modifier, contentPadding = 12.dp, cornerRadius = 22.dp) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                color = TextPrimary,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "BPM",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 5.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusBadge(label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text("● $label", style = MaterialTheme.typography.labelSmall, color = color)
    }
}

private fun periodLabel(p: Period): String = when (p) {
    Period.D -> "today"
    Period.W -> "7 days"
    Period.M -> "30 days"
    Period.SixM -> "6 months"
    Period.Y -> "year"
}

private fun friendlyTime(at: Instant): String =
    DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault()).format(at)

private fun friendlyTimestamp(at: Instant): String {
    val mins = Duration.between(at, Instant.now()).toMinutes()
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "$mins min ago"
        mins < 1440 -> "${mins / 60}h ago, ${friendlyTime(at)}"
        else -> friendlyTime(at)
    }
}
