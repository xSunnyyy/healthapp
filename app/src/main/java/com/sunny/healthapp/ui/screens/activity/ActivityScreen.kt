package com.sunny.healthapp.ui.screens.activity

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.ui.components.EditorialHeader
import com.sunny.healthapp.ui.components.Panel
import com.sunny.healthapp.ui.components.RingProgress
import com.sunny.healthapp.ui.components.StaggeredEnter
import com.sunny.healthapp.ui.components.WeekStrip
import com.sunny.healthapp.ui.screens.PermissionGate
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.AccentDeep
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.Lavender
import com.sunny.healthapp.ui.theme.MintGlow
import com.sunny.healthapp.ui.theme.Sunflare
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary
import java.time.LocalDate

@Composable
fun ActivityScreen() {
    PermissionGate {
        val app = LocalContext.current.applicationContext as HealthApp
        val vm: ActivityViewModel = viewModel(factory = ActivityViewModel.factory(app))
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
private fun Content(state: ActivityState) {
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = statusInset + 12.dp, bottom = 130.dp),
    ) {
        StaggeredEnter(0) { m ->
            EditorialHeader(
                eyebrow = "Progress tracking",
                title = "Your week,\nin motion.",
                modifier = m,
            )
        }
        Spacer(Modifier.height(20.dp))

        StaggeredEnter(1) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                WeekStrip(
                    selected = selectedDate,
                    onSelect = { selectedDate = it },
                    activeDays = state.recent.map { it.date }.toSet(),
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        StaggeredEnter(2) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Text("Goal Progress", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val steps = state.today?.steps ?: 0L
                        val progress = (steps / 10_000f).coerceIn(0f, 1f)
                        val pct = (progress * 100).toInt()
                        RingProgress(
                            progress = progress,
                            color = Accent,
                            gradientEnd = Lavender,
                            diameter = 110.dp,
                            strokeWidth = 10.dp,
                            centerLabel = "$pct%",
                            centerCaption = "Score",
                        )
                        Spacer(Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Critical range",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Keep going to reach your weekly benchmark",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                            Spacer(Modifier.height(10.dp))
                            DeltaChip("+14% vs yesterday", MintGlow)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        StaggeredEnter(3) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Text("Activity", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        StatColumn("+23%", "vs yesterday", MintGlow, Modifier.weight(1f))
                        StatColumn(
                            (state.today?.totalCalories ?: 0.0).toInt().toString(),
                            "Total burned",
                            Accent,
                            Modifier.weight(1f),
                            unit = "kcal",
                        )
                        StatColumn(
                            ((state.today?.totalCalories ?: 0.0) / 1.5).toInt().toString(),
                            "Daily avg",
                            Lavender,
                            Modifier.weight(1f),
                            unit = "kcal",
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    StackedActivityBars(state)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        LegendDot("Walking 65%", Accent)
                        LegendDot("Running 11%", Crimson)
                        LegendDot("Workout 24%", Lavender)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        StaggeredEnter(4) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Text("Daily Goals", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(14.dp))
                    GoalRow(
                        label = "Steps",
                        current = (state.today?.steps ?: 0L).toString(),
                        target = "10,000",
                        progress = ((state.today?.steps ?: 0L) / 10_000f).coerceIn(0f, 1f),
                        accent = Accent,
                        done = (state.today?.steps ?: 0L) >= 10_000,
                    )
                    Spacer(Modifier.height(14.dp))
                    GoalRow(
                        label = "Active minutes",
                        current = (state.today?.exerciseMinutes ?: 0L).toString(),
                        target = "30",
                        progress = ((state.today?.exerciseMinutes ?: 0L) / 30f).coerceIn(0f, 1f),
                        accent = Sunflare,
                        done = (state.today?.exerciseMinutes ?: 0L) >= 30,
                    )
                    Spacer(Modifier.height(14.dp))
                    GoalRow(
                        label = "Calories",
                        current = (state.today?.totalCalories ?: 0.0).toInt().toString(),
                        target = "2,500",
                        progress = ((state.today?.totalCalories ?: 0.0) / 2_500.0).toFloat().coerceIn(0f, 1f),
                        accent = Lavender,
                        done = (state.today?.totalCalories ?: 0.0) >= 2_500,
                    )
                }
            }
        }
    }
}

@Composable
private fun StackedActivityBars(state: ActivityState) {
    val maxVal = (state.recent.maxOfOrNull { it.totalCalories } ?: 1.0).coerceAtLeast(1.0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        // Show last 7 days reversed (oldest left)
        val days = state.recent.reversed()
        days.forEach { day ->
            val total = (day.totalCalories / maxVal).toFloat().coerceIn(0.05f, 1f)
            val walking = total * 0.65f
            val running = total * 0.11f
            val workout = total * 0.24f
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(110.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                    ) {
                        val cw = size.width
                        val ch = size.height
                        val totalH = ch * total
                        val workH = ch * workout
                        val runH = ch * running
                        val walkH = ch * walking
                        val baseY = ch
                        val r = cw / 2f
                        // walking at bottom
                        drawRoundRect(
                            color = Accent,
                            topLeft = Offset(0f, baseY - walkH),
                            size = Size(cw, walkH),
                            cornerRadius = CornerRadius(0f, 0f),
                        )
                        // running on top
                        drawRoundRect(
                            color = Crimson,
                            topLeft = Offset(0f, baseY - walkH - runH),
                            size = Size(cw, runH),
                            cornerRadius = CornerRadius(0f, 0f),
                        )
                        // workout on top
                        drawRoundRect(
                            color = Lavender,
                            topLeft = Offset(0f, baseY - totalH),
                            size = Size(cw, workH),
                            cornerRadius = CornerRadius(r, r),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    unit: String? = null,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            if (unit != null) {
                Spacer(Modifier.width(3.dp))
                Text(unit, style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.padding(bottom = 5.dp))
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

@Composable
private fun GoalRow(
    label: String,
    current: String,
    target: String,
    progress: Float,
    accent: Color,
    done: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (done) accent else TextMuted,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                Text(
                    text = "$current / $target",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.08f),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                )
                drawRoundRect(
                    color = accent,
                    size = Size(size.width * progress, size.height),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                )
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun DeltaChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text("● $label", style = MaterialTheme.typography.labelSmall, color = color)
    }
}
