package com.sunny.healthapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.sunny.healthapp.ui.components.BarChart7Day
import com.sunny.healthapp.ui.components.BarPoint
import com.sunny.healthapp.ui.components.DateScrubber
import com.sunny.healthapp.ui.components.EditorialHeader
import com.sunny.healthapp.ui.components.GradientTile
import com.sunny.healthapp.ui.components.GradientTileRow
import com.sunny.healthapp.ui.components.MiniSparkline
import com.sunny.healthapp.ui.components.Panel
import com.sunny.healthapp.ui.components.StaggeredEnter
import com.sunny.healthapp.ui.screens.PermissionGate
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.Lavender
import com.sunny.healthapp.ui.theme.LavenderDeep
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary
import com.sunny.healthapp.ui.theme.TileCoolEnd
import com.sunny.healthapp.ui.theme.TileCoolStart
import com.sunny.healthapp.ui.theme.TileSoftEnd
import com.sunny.healthapp.ui.theme.TileSoftStart
import com.sunny.healthapp.ui.theme.TileWarmEnd
import com.sunny.healthapp.ui.theme.TileWarmStart
import java.time.LocalDate

@Composable
fun HomeScreen(onNavigate: (String) -> Unit = {}) {
    PermissionGate {
        val app = LocalContext.current.applicationContext as HealthApp
        val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
        val state by vm.state.collectAsStateWithLifecycle()
        HomeContent(state, vm, onNavigate)
    }
}

@Composable
private fun HomeContent(state: HomeState, vm: HomeViewModel, onNavigate: (String) -> Unit) {
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
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
            ) {
                DateScrubber(
                    date = state.date,
                    onPrevious = { vm.previous() },
                    onNext = { vm.next() },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

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
                ReadinessPanel(state)
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
    val prev = state.previousDaily
    val stepsDelta = deltaPct(daily?.steps?.toDouble(), prev?.steps?.toDouble())
    val calDelta = deltaPct(daily?.totalCalories, prev?.totalCalories)
    val sleepMin = state.sleep?.total?.toMinutes()
    GradientTileRow(
        tiles = listOf(
            { mod ->
                GradientTile(
                    label = "Steps",
                    value = "%,d".format(daily?.steps ?: 0L),
                    delta = stepsDelta,
                    glowStart = TileCoolStart,
                    glowEnd = TileCoolEnd,
                    modifier = mod,
                    onClick = { onNavigate("activity") },
                )
            },
            { mod ->
                GradientTile(
                    label = "Calories",
                    value = "%,d".format((daily?.totalCalories ?: 0.0).toInt()),
                    delta = calDelta,
                    glowStart = TileWarmStart,
                    glowEnd = TileWarmEnd,
                    modifier = mod,
                    onClick = { onNavigate("activity") },
                )
            },
            { mod ->
                GradientTile(
                    label = "Sleep",
                    value = sleepMin?.let { formatSleepShort(it) } ?: "—",
                    delta = state.sleep?.efficiencyPct?.let { "$it% eff" },
                    glowStart = TileSoftStart,
                    glowEnd = TileSoftEnd,
                    modifier = mod,
                    onClick = { onNavigate("sleep") },
                )
            },
        ),
    )
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

private fun deltaPct(current: Double?, previous: Double?): String? {
    if (current == null || previous == null || previous <= 0.0) return null
    val pct = ((current - previous) / previous * 100).toInt()
    return when {
        pct == 0 -> "On par"
        pct > 0 -> "+$pct% vs prev"
        else -> "$pct% vs prev"
    }
}
