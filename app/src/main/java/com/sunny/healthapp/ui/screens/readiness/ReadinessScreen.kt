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
import com.sunny.healthapp.ui.components.EditorialHeader
import com.sunny.healthapp.ui.components.LinePoint
import com.sunny.healthapp.ui.components.Panel
import com.sunny.healthapp.ui.components.Period
import com.sunny.healthapp.ui.components.PeriodTabs
import com.sunny.healthapp.ui.components.SmoothLineChart
import com.sunny.healthapp.ui.components.StaggeredEnter
import com.sunny.healthapp.ui.components.ZoneBar
import com.sunny.healthapp.ui.screens.PermissionGate
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.MintGlow
import com.sunny.healthapp.ui.theme.Sunflare
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary

@Composable
fun ReadinessScreen() {
    PermissionGate {
        val app = LocalContext.current.applicationContext as HealthApp
        val vm: ReadinessViewModel = viewModel(factory = ReadinessViewModel.factory(app))
        val state by vm.state.collectAsStateWithLifecycle()
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent, strokeWidth = 2.dp)
            }
        } else {
            Content(state)
        }
    }
}

@Composable
private fun Content(state: ReadinessState) {
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
                eyebrow = "Heart rate · readiness",
                title = "Today's heart\nin one glance",
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
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Heart Rate", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "78",
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
                                text = "Today, 9:38 AM",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Avg today", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("94", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                                Spacer(Modifier.width(4.dp))
                                Text("BPM", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            ElevationBadge(label = "Elevated")
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    val (values, axis) = chartDataFor(period)
                    val points = values.mapIndexed { i, v -> LinePoint(i.toFloat(), v) }
                    SmoothLineChart(points = points, color = Crimson, height = 130.dp)
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        axis.forEach {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        StaggeredEnter(3) { m ->
            Row(
                modifier = m.padding(horizontal = 20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniStat("Average", "84", "Today", Modifier.weight(1f))
                MiniStat("Resting HR", "62", "30-day avg", Modifier.weight(1f))
                MiniStat("Max today", "138", "2:14 PM", Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(18.dp))

        StaggeredEnter(4) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Text("Heart Rate Zones", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    ZoneBar("Normal", "60–100 bpm", 60, MintGlow)
                    Spacer(Modifier.height(14.dp))
                    ZoneBar("Elevated", "100–130 bpm", 19, Sunflare)
                    Spacer(Modifier.height(14.dp))
                    ZoneBar("High", "130+ bpm", 21, Crimson)
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Panel(modifier = modifier, contentPadding = 14.dp, cornerRadius = 22.dp) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Spacer(Modifier.width(4.dp))
            Text("BPM", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.padding(bottom = 5.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text(sub, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

private fun chartDataFor(period: Period): Pair<List<Float>, List<String>> = when (period) {
    Period.D -> listOf(72f, 74f, 78f, 86f, 92f, 88f, 81f, 76f, 84f, 92f, 100f, 96f, 88f, 80f, 78f, 76f) to
        listOf("6AM", "12PM", "3PM", "6PM", "Now")
    Period.W -> listOf(76f, 82f, 79f, 88f, 91f, 84f, 78f) to listOf("M", "T", "W", "T", "F", "S", "S")
    Period.M -> List(30) { (70 + (Math.sin(it * 0.4) * 12 + Math.random() * 5)).toFloat() } to
        listOf("Week 1", "Week 2", "Week 3", "Week 4")
    Period.SixM -> List(24) { (70 + Math.sin(it * 0.3) * 10).toFloat() } to
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
    Period.Y -> List(12) { (72 + Math.cos(it * 0.5) * 8).toFloat() } to
        listOf("Q1", "Q2", "Q3", "Q4")
}

@Composable
private fun ElevationBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Sunflare.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "● $label",
            style = MaterialTheme.typography.labelSmall,
            color = Sunflare,
        )
    }
}
