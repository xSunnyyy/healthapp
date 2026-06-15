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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.domain.model.DailySummary
import com.sunny.healthapp.ui.components.EditorialHeader
import com.sunny.healthapp.ui.components.Panel
import com.sunny.healthapp.ui.components.RingProgress
import com.sunny.healthapp.ui.components.StaggeredEnter
import com.sunny.healthapp.ui.components.SyncDot
import com.sunny.healthapp.ui.components.WeekStrip
import com.sunny.healthapp.ui.screens.PermissionGate
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.Lavender
import com.sunny.healthapp.ui.theme.MintGlow
import com.sunny.healthapp.ui.theme.Sunflare
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ActivityScreen() {
    PermissionGate {
        val app = LocalContext.current.applicationContext as HealthApp
        val vm: ActivityViewModel = viewModel(factory = ActivityViewModel.factory(app))
        val state by vm.state.collectAsStateWithLifecycle()
        val sync by vm.syncStatus.collectAsStateWithLifecycle()
        if (state.loading && state.selected == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent, strokeWidth = 2.dp)
            }
        } else {
            Content(state, sync, onSelectDate = vm::setDate, onSync = vm::manualSync)
        }
    }
}

@Composable
private fun Content(
    state: ActivityState,
    sync: com.sunny.healthapp.data.sync.SyncStatus,
    onSelectDate: (LocalDate) -> Unit,
    onSync: () -> Unit,
) {
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val daily = state.selected
    val prev = state.previousDay

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = statusInset + 12.dp, bottom = 130.dp),
    ) {
        StaggeredEnter(0) { m ->
            EditorialHeader(
                eyebrow = "Progress tracking",
                title = dateHeadline(state.date),
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
                WeekStrip(
                    selected = state.date,
                    onSelect = onSelectDate,
                    activeDays = state.recent.map { it.date }.toSet(),
                    modifier = Modifier.weight(1f),
                )
                SyncDot(status = sync, onClick = onSync)
            }
        }
        Spacer(Modifier.height(20.dp))

        StaggeredEnter(2) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Text("Goal Progress", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val steps = daily?.steps ?: 0L
                        val progress = (steps / 10_000f).coerceIn(0f, 1f)
                        val pct = (progress * 100).toInt()
                        RingProgress(
                            progress = progress,
                            color = Accent,
                            gradientEnd = Lavender,
                            diameter = 110.dp,
                            strokeWidth = 10.dp,
                            centerLabel = "$pct%",
                            centerCaption = "Goal",
                        )
                        Spacer(Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                rangeBlurb(pct),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                rangeSubtitle(steps),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                            Spacer(Modifier.height(10.dp))
                            val (delta, color) = stepDelta(daily?.steps, prev?.steps)
                            if (delta != null) DeltaChip(delta, color)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        StaggeredEnter(3) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Text("Calories burned", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        val (calDelta, _) = stepDelta(
                            (daily?.totalCalories ?: 0.0).toLong(),
                            (prev?.totalCalories ?: 0.0).toLong(),
                        )
                        StatColumn(calDelta ?: "—", "vs ${prevLabel(state.date)}", MintGlow, Modifier.weight(1f))
                        StatColumn(
                            "%,d".format((daily?.totalCalories ?: 0.0).toInt()),
                            "Total burned",
                            Accent,
                            Modifier.weight(1f),
                            unit = "kcal",
                        )
                        val weeklyAvg = (state.recent.sumOf { it.totalCalories } / state.recent.size.coerceAtLeast(1))
                        StatColumn(
                            "%,d".format(weeklyAvg.toInt()),
                            "7-day avg",
                            Lavender,
                            Modifier.weight(1f),
                            unit = "kcal",
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    WeeklyCalorieBars(state)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Total calories burned each day",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        StaggeredEnter(4) { m ->
            Box(modifier = m.padding(horizontal = 20.dp)) {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Daily Goals · ${dateHeadlineShort(state.date)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(14.dp))
                    GoalRow(
                        label = "Steps",
                        current = "%,d".format(daily?.steps ?: 0L),
                        target = "10,000",
                        progress = ((daily?.steps ?: 0L) / 10_000f).coerceIn(0f, 1f),
                        accent = Accent,
                        done = (daily?.steps ?: 0L) >= 10_000,
                    )
                    Spacer(Modifier.height(14.dp))
                    GoalRow(
                        label = "Active minutes",
                        current = (daily?.exerciseMinutes ?: 0L).toString(),
                        target = "30",
                        progress = ((daily?.exerciseMinutes ?: 0L) / 30f).coerceIn(0f, 1f),
                        accent = Sunflare,
                        done = (daily?.exerciseMinutes ?: 0L) >= 30,
                    )
                    Spacer(Modifier.height(14.dp))
                    GoalRow(
                        label = "Calories",
                        current = "%,d".format((daily?.totalCalories ?: 0.0).toInt()),
                        target = "2,500",
                        progress = ((daily?.totalCalories ?: 0.0) / 2_500.0).toFloat().coerceIn(0f, 1f),
                        accent = Lavender,
                        done = (daily?.totalCalories ?: 0.0) >= 2_500,
                    )
                    Spacer(Modifier.height(14.dp))
                    GoalRow(
                        label = "Distance",
                        current = "%.2f km".format((daily?.distanceMeters ?: 0.0) / 1000.0),
                        target = "5 km",
                        progress = (((daily?.distanceMeters ?: 0.0) / 1000.0) / 5.0).toFloat().coerceIn(0f, 1f),
                        accent = Crimson,
                        done = ((daily?.distanceMeters ?: 0.0) / 1000.0) >= 5.0,
                    )
                }
            }
        }
    }
}

private fun dateHeadline(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today's\nprogress."
        today.minusDays(1) -> "Yesterday's\nprogress."
        else -> date.format(DateTimeFormatter.ofPattern("EEEE,\nMMM d", Locale.getDefault()))
    }
}

private fun dateHeadlineShort(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    }
}

private fun prevLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "yesterday"
        today.minusDays(1) -> "2 days ago"
        else -> "prev day"
    }
}

private fun stepDelta(now: Long?, then: Long?): Pair<String?, Color> {
    if (now == null || then == null || then <= 0L) return null to Color.Transparent
    val pct = ((now - then).toDouble() / then * 100).toInt()
    return when {
        pct == 0 -> "On par" to TextSecondary
        pct > 0 -> "+$pct% vs prev" to MintGlow
        else -> "$pct% vs prev" to Crimson
    }
}

private fun rangeBlurb(pct: Int): String = when {
    pct >= 100 -> "Goal complete"
    pct >= 70 -> "Almost there"
    pct >= 40 -> "On your way"
    pct > 0 -> "Just getting going"
    else -> "Nothing yet"
}

private fun rangeSubtitle(steps: Long): String = when {
    steps >= 10_000 -> "You hit your daily target."
    steps > 0 -> "${10_000 - steps} steps to your goal."
    else -> "Move around to start tracking."
}

@Composable
private fun WeeklyCalorieBars(state: ActivityState) {
    val maxVal = (state.recent.maxOfOrNull { it.totalCalories } ?: 1.0).coerceAtLeast(1.0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        val days = state.recent
        days.forEach { day ->
            val ratio = (day.totalCalories / maxVal).toFloat().coerceIn(0.05f, 1f)
            val isSelected = day.date == state.date
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(110.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxWidth().height(110.dp * ratio),
                    ) {
                        val r = size.width / 2f
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                0.0f to (if (isSelected) Accent else Accent.copy(alpha = 0.45f)),
                                1.0f to (if (isSelected) Accent.copy(alpha = 0.65f) else Accent.copy(alpha = 0.18f)),
                            ),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(r, r),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Accent else TextMuted,
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
