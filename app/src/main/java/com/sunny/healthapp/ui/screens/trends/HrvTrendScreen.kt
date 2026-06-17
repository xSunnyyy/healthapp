package com.sunny.healthapp.ui.screens.trends

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.ui.components.LinePoint
import com.sunny.healthapp.ui.components.Panel
import com.sunny.healthapp.ui.components.SmoothLineChart
import com.sunny.healthapp.ui.components.SyncDot
import com.sunny.healthapp.ui.theme.Lavender
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary
import com.sunny.healthapp.ui.util.hapticClickable

@Composable
fun HrvTrendScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as HealthApp
    val vm: HrvTrendViewModel = viewModel(factory = HrvTrendViewModel.factory(app))
    val state by vm.state.collectAsStateWithLifecycle()
    val sync by vm.syncStatus.collectAsStateWithLifecycle()

    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = statusInset + 12.dp, bottom = 40.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .hapticClickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "HRV trend",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            SyncDot(status = sync, onClick = { vm.manualSync() }, size = 30.dp)
        }
        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            Panel(modifier = Modifier.fillMaxWidth()) {
                Text("Last 30 nights", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = state.latest?.toInt()?.toString() ?: "—",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "ms",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = state.baseline?.toInt()
                        ?.let { "30-night baseline ${it} ms" }
                        ?: "Need a few nights to calibrate",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
                Spacer(Modifier.height(18.dp))
                if (state.points.size >= 2) {
                    val points = state.points.mapIndexed { i, p -> LinePoint(i.toFloat(), p.value.toFloat()) }
                    SmoothLineChart(
                        points = points,
                        color = Lavender,
                        height = 170.dp,
                        fillBelow = true,
                        drawDot = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("30d ago", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("today", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.02f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Not enough nights yet — wear your Fitbit overnight to start building the trend.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            Panel(modifier = Modifier.fillMaxWidth()) {
                Text("About HRV", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Heart-rate variability (HRV-RMSSD) measures the small timing differences " +
                        "between heartbeats. Higher numbers usually mean a calmer, more " +
                        "recovered autonomic nervous system. Watch the trend more than any " +
                        "individual reading — sustained drops vs. your own baseline often " +
                        "signal stress, illness, or accumulated training load.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}
