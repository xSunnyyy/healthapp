package com.sunny.healthapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.domain.model.SleepSummary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.LocalFireDepartment
import com.sunny.healthapp.ui.components.BarChart7Day
import com.sunny.healthapp.ui.components.BarPoint
import com.sunny.healthapp.ui.components.DateScrubber
import com.sunny.healthapp.ui.components.EditorialHeader
import com.sunny.healthapp.ui.components.BodyBatteryPanel
import com.sunny.healthapp.ui.components.InsightsPanel
import com.sunny.healthapp.ui.components.MiniArea
import com.sunny.healthapp.ui.components.MiniBars
import com.sunny.healthapp.ui.components.MiniSleepStrip
import com.sunny.healthapp.ui.components.MiniSparkline
import com.sunny.healthapp.ui.components.Panel
import com.sunny.healthapp.ui.components.RefreshableContent
import com.sunny.healthapp.ui.components.StaggeredEnter
import com.sunny.healthapp.ui.components.StatTile
import com.sunny.healthapp.ui.components.StatTileRow
import com.sunny.healthapp.ui.components.SyncDot
import com.sunny.healthapp.ui.components.SyncErrorBanner
import com.sunny.healthapp.ui.screens.PermissionGate
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.Lavender
import com.sunny.healthapp.ui.theme.LavenderDeep
import com.sunny.healthapp.ui.theme.Sunflare
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary
import java.time.LocalDate

@Composable
fun HomeScreen(onNavigate: (String) -> Unit = {}) {
    PermissionGate {
        val app = LocalContext.current.applicationContext as HealthApp
        val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
        val state by vm.state.collectAsStateWithLifecycle()
        val syncStatus by vm.syncStatus.collectAsStateWithLifecycle()
        HomeContent(state, syncStatus, vm, onNavigate)
    }
}

@Composable
private fun HomeContent(
    state: HomeState,
    syncStatus: com.sunny.healthapp.data.sync.SyncStatus,
    vm: HomeViewModel,
    onNavigate: (String) -> Unit,
) {
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isRefreshing = syncStatus is com.sunny.healthapp.data.sync.SyncStatus.Syncing

    RefreshableContent(
        isRefreshing = isRefreshing,
        onRefresh = { vm.manualSync() },
        modifier = Modifier.fillMaxSize(),
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = statusInset + 12.dp, bottom = 130.dp),
    ) {
        StaggeredEnter(index = 0) { m ->
            Row(
                modifier = m.padding(horizontal = 20.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DateScrubber(
                    date = state.date,
                    onPrevious = { vm.previous() },
                    onNext = { vm.next() },
                    onJumpToToday = { vm.goToDate(java.time.LocalDate.now()) },
                    modifier = Modifier.weight(1f),
                )
                SyncDot(
                    status = syncStatus,
                    onClick = { vm.manualSync() },
                    size = 30.dp,
                )
                SettingsIcon(onClick = { onNavigate("settings") })
            }
        }

        Spacer(Modifier.height(24.dp))

        (syncStatus as? com.sunny.healthapp.data.sync.SyncStatus.Error)?.let { err ->
            StaggeredEnter(index = 1) { m ->
                Box(modifier = m.padding(horizontal = 20.dp)) {
                    SyncErrorBanner(message = err.message ?: "Sync failed.")
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        StaggeredEnter(index = 1) { m ->
            EditorialHeader(
                eyebrow = headerEyebrow(state.date),
                title = headerTitle(state.date),
                modifier = m,
            )
        }

        Spacer(Modifier.height(22.dp))

        if (state.loading && state.daily == null) {
            Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent, strokeWidth = 2.dp)
            }
            return@Column
        }

        StaggeredEnter(index = 2) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                TopMetricRow(state, onNavigate)
            }
        }

        Spacer(Modifier.height(16.dp))

        StaggeredEnter(index = 3) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                StepsPanel(state) { date -> vm.goToDate(date) }
            }
        }

        Spacer(Modifier.height(16.dp))

        StaggeredEnter(index = 4) { m ->
            Row(
                modifier = m
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                HeartRatePanel(
                    state,
                    onClick = { onNavigate("readiness") },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                SleepPanel(
                    state.sleep,
                    onClick = { onNavigate("sleep") },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        StaggeredEnter(index = 5) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                state.bodyBattery?.let { BodyBatteryPanel(summary = it) }
            }
        }

        state.insights?.let { insights ->
            Spacer(Modifier.height(16.dp))
            StaggeredEnter(index = 6) { m ->
                Box(modifier = m.padding(horizontal = 20.dp)) {
                    InsightsPanel(insights = insights)
                }
            }
        }
    }
    }
}

private fun headerEyebrow(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Daily summary · Today"
        today.minusDays(1) -> "Daily summary · Yesterday"
        else -> "Daily summary"
    }
}

private fun headerTitle(date: LocalDate): String {
    val today = LocalDate.now()
    val fmt = java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.getDefault())
    return when (date) {
        today -> "Your health\nsummary today"
        today.minusDays(1) -> "Your health\nsummary yesterday"
        else -> "Your health\nsummary on ${date.format(fmt)}"
    }
}

@Composable
private fun TopMetricRow(state: HomeState, onNavigate: (String) -> Unit) {
    val daily = state.daily
    val sleepMin = state.sleep?.total?.toMinutes()
    val stepsBars = state.weeklySteps.map { it.second.toFloat() }
    val calBars = state.weeklyCalories.map { it.second.toFloat() }
    val hiIdx = state.weeklySteps.indexOfFirst { it.first == state.date }.takeIf { it >= 0 }

    StatTileRow(
        tiles = listOf(
            { mod ->
                StatTile(
                    icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    label = "Steps",
                    value = "%,d".format(daily?.steps ?: 0L),
                    status = stepsStatus(daily?.steps ?: 0L),
                    accent = Accent,
                    chart = {
                        // Vertical bars — daily totals over the past week
                        MiniBars(
                            values = stepsBars,
                            color = Accent,
                            height = 30.dp,
                            highlightIndex = hiIdx,
                        )
                    },
                    modifier = mod,
                    onClick = { onNavigate("activity") },
                )
            },
            { mod ->
                StatTile(
                    icon = Icons.Outlined.LocalFireDepartment,
                    label = "Calories",
                    value = "%,d".format((daily?.totalCalories ?: 0.0).toInt()),
                    unit = "kcal",
                    status = caloriesStatus((daily?.totalCalories ?: 0.0).toInt()),
                    accent = Sunflare,
                    chart = {
                        // Filled smooth area — emphasises the trend
                        MiniArea(values = calBars, color = Sunflare, height = 30.dp)
                    },
                    modifier = mod,
                    onClick = { onNavigate("activity") },
                )
            },
            { mod ->
                StatTile(
                    icon = Icons.Outlined.Bedtime,
                    label = "Sleep",
                    value = sleepMin?.let { formatSleep(it) } ?: "—",
                    status = sleepStatus(sleepMin),
                    accent = Lavender,
                    chart = {
                        // Last night's stage breakdown as a colored ribbon
                        MiniSleepStrip(sleep = state.sleep, height = 18.dp)
                    },
                    modifier = mod,
                    onClick = { onNavigate("sleep") },
                )
            },
        ),
    )
}

private fun stepsStatus(steps: Long): String = when {
    steps <= 0L -> "No data"
    steps >= 10_000 -> "Goal hit"
    steps >= 7_000 -> "Active"
    steps >= 3_000 -> "Moving"
    else -> "Light day"
}

private fun caloriesStatus(cal: Int): String = when {
    cal <= 0 -> "No data"
    cal >= 2_800 -> "Big day"
    cal >= 2_200 -> "Active burn"
    cal >= 1_500 -> "On track"
    else -> "Light day"
}

private fun sleepStatus(minutes: Long?): String {
    val m = minutes ?: return "No data"
    if (m <= 0L) return "No data"
    val h = m / 60.0
    return when {
        h >= 9.5 -> "Long"
        h >= 7 -> "Optimal"
        h >= 6 -> "Short"
        else -> "Very short"
    }
}

@Composable
private fun StepsPanel(state: HomeState, onTap: (java.time.LocalDate) -> Unit) {
    val daily = state.daily
    val steps = daily?.steps ?: 0L
    val goal = 10_000L
    val remaining = (goal - steps).coerceAtLeast(0)

    Panel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Steps".uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "GOAL  10,000",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "%,d".format(steps),
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "steps",
                style = MaterialTheme.typography.displaySmall,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (remaining > 0) "%,d remaining".format(remaining) else "Goal reached",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
        Spacer(Modifier.height(20.dp))

        val points = state.weeklySteps.map { (d, v) ->
            BarPoint(label = d.dayOfWeek.name.take(1), value = v.toFloat())
        }
        val highlight = state.weeklySteps.indexOfFirst { it.first == state.date }
            .takeIf { it >= 0 }
        BarChart7Day(
            points = points,
            highlightIndex = highlight,
            color = Accent,
            onBarClick = { idx ->
                state.weeklySteps.getOrNull(idx)?.first?.let { d -> onTap(d) }
            },
        )
    }
}

@Composable
private fun HeartRatePanel(state: HomeState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hr = state.daily?.latestHeartRate
        ?: state.daily?.avgHeartRate
        ?: state.daily?.restingHeartRate
    Panel(modifier = modifier.clickable(onClick = onClick), contentPadding = 18.dp) {
        Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Heart Rate", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = hr?.toString() ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "BPM",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (state.date == LocalDate.now()) "Resting" else "Avg",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
            Spacer(Modifier.height(14.dp))
            val sparkline = listOf(72f, 76f, 78f, 74f, 80f, 84f, 78f, 76f, 82f, 78f)
            MiniSparkline(values = sparkline, color = Crimson, height = 44.dp)
        }
    }
}

@Composable
private fun SleepPanel(sleep: SleepSummary?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Panel(modifier = modifier.clickable(onClick = onClick), contentPadding = 18.dp) {
        Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Sleep", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = sleep?.let { formatSleepShort(it.total.toMinutes()) } ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Hrs & Min",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            SleepRibbon(sleep)
        }
    }
}

@Composable
private fun SleepRibbon(sleep: SleepSummary?) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val deep = sleep?.deep?.toMinutes()?.toFloat() ?: 0f
            val light = sleep?.light?.toMinutes()?.toFloat() ?: 0f
            val rem = sleep?.rem?.toMinutes()?.toFloat() ?: 0f
            val awake = sleep?.awake?.toMinutes()?.toFloat() ?: 0f
            val total = (deep + light + rem + awake).coerceAtLeast(1f)
            RibbonSegment(weight = deep / total, color = LavenderDeep)
            RibbonSegment(weight = light / total, color = Accent)
            RibbonSegment(weight = rem / total, color = Lavender)
            RibbonSegment(weight = awake / total, color = Crimson)
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Text("11pm", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.weight(1f))
            Text("6:24am", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
private fun RowScope.RibbonSegment(weight: Float, color: Color) {
    if (weight <= 0f) return
    Box(
        modifier = Modifier
            .weight(weight.coerceAtLeast(0.001f))
            .height(10.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color),
    )
}

@Composable
private fun ReadinessPanel(state: HomeState) {
    val r = state.readiness
    Panel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Readiness",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (state.date == LocalDate.now()) "TODAY" else "SELECTED",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = r?.score?.toString() ?: "—",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "/100",
                style = MaterialTheme.typography.displaySmall,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = verdict(r?.score ?: 0),
            style = MaterialTheme.typography.displaySmall,
            color = TextSecondary,
        )
    }
}

private fun verdict(score: Int): String = when {
    score >= 85 -> "Primed for the day."
    score >= 70 -> "A solid baseline."
    score >= 55 -> "Take it a bit easier."
    score > 0 -> "Lean into recovery."
    else -> "—"
}

private fun formatSleep(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return "${h}h ${m}m"
}

private fun formatSleepShort(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return "${h}:%02d".format(m)
}

@Composable
private fun SettingsIcon(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "Settings",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun deltaPct(current: Double?, previous: Double?): String? {
    if (current == null || previous == null || previous <= 0.0) return null
    val pct = ((current - previous) / previous * 100).toInt()
    return when {
        pct == 0 -> "on par"
        pct > 0 -> "+$pct% prev"
        else -> "$pct% prev"
    }
}
