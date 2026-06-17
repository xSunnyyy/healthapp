package com.sunny.healthapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.ui.screens.home.WeeklyInsights
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.Lavender
import com.sunny.healthapp.ui.theme.MintGlow
import com.sunny.healthapp.ui.theme.Sunflare
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary

@Composable
fun InsightsPanel(
    insights: WeeklyInsights,
    modifier: Modifier = Modifier,
) {
    Panel(modifier = modifier.fillMaxWidth()) {
        Text("This week", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(2.dp))
        Text(
            text = headlineLine(insights),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(16.dp))

        // Sleep
        InsightRow(
            accent = Lavender,
            label = "Sleep avg",
            value = formatDuration(insights.avgSleepMinThisWeek),
            trend = sleepTrend(insights),
        )
        Divider()

        // Steps goal completion
        InsightRow(
            accent = Accent,
            label = "Step goal",
            value = "${insights.stepsGoalHitDays} of 7 days",
            trend = stepsTrend(insights),
        )
        Divider()

        // Resting HR
        InsightRow(
            accent = Crimson,
            label = "Resting HR",
            value = insights.rhrAvgThisWeek?.let { "$it bpm" } ?: "—",
            trend = rhrTrend(insights),
        )
        Divider()

        // Avg steps
        InsightRow(
            accent = Sunflare,
            label = "Avg steps",
            value = "%,d".format(insights.avgStepsThisWeek),
            trend = avgStepsTrend(insights),
        )
    }
}

private data class Trend(val text: String, val icon: ImageVector, val color: Color)

private fun sleepTrend(i: WeeklyInsights): Trend? {
    val d = i.sleepDeltaMin ?: return null
    return when {
        d > 5 -> Trend("+${formatDuration(d)} vs last week", Icons.AutoMirrored.Outlined.TrendingUp, MintGlow)
        d < -5 -> Trend("${formatDuration(d)} vs last week", Icons.AutoMirrored.Outlined.TrendingDown, Crimson)
        else -> Trend("steady", Icons.AutoMirrored.Outlined.TrendingFlat, TextSecondary)
    }
}

private fun stepsTrend(i: WeeklyInsights): Trend? = when {
    i.stepsGoalHitDays >= 5 -> Trend("on a roll", Icons.AutoMirrored.Outlined.TrendingUp, MintGlow)
    i.stepsGoalHitDays >= 3 -> Trend("solid pace", Icons.AutoMirrored.Outlined.TrendingFlat, TextSecondary)
    i.stepsGoalHitDays >= 1 -> Trend("a few wins", Icons.AutoMirrored.Outlined.TrendingFlat, Sunflare)
    else -> Trend("none yet", Icons.AutoMirrored.Outlined.TrendingDown, Crimson)
}

private fun rhrTrend(i: WeeklyInsights): Trend? {
    val d = i.rhrDelta ?: return null
    return when {
        // Lower resting HR is generally better
        d <= -2 -> Trend("${d} bpm vs last week", Icons.AutoMirrored.Outlined.TrendingDown, MintGlow)
        d >= 3 -> Trend("+$d bpm vs last week", Icons.AutoMirrored.Outlined.TrendingUp, Sunflare)
        else -> Trend("steady", Icons.AutoMirrored.Outlined.TrendingFlat, TextSecondary)
    }
}

private fun avgStepsTrend(i: WeeklyInsights): Trend? {
    val last = i.avgStepsLastWeek ?: return null
    val pct = if (last <= 0) 0 else (((i.avgStepsThisWeek - last) * 100) / last)
    return when {
        pct >= 8 -> Trend("+$pct% vs last week", Icons.AutoMirrored.Outlined.TrendingUp, MintGlow)
        pct <= -8 -> Trend("$pct% vs last week", Icons.AutoMirrored.Outlined.TrendingDown, Crimson)
        else -> Trend("on par", Icons.AutoMirrored.Outlined.TrendingFlat, TextSecondary)
    }
}

@Composable
private fun InsightRow(
    accent: Color,
    label: String,
    value: String,
    trend: Trend?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            if (trend != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(
                        imageVector = trend.icon,
                        contentDescription = null,
                        tint = trend.color,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        trend.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = trend.color,
                    )
                }
            }
        }
        Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color.White.copy(alpha = 0.05f)),
    )
}

private fun formatDuration(minutes: Long): String {
    val absMin = kotlin.math.abs(minutes)
    val h = absMin / 60
    val m = absMin % 60
    val s = if (h > 0) "${h}h ${m}m" else "${m}m"
    return if (minutes < 0) "-$s" else s
}

private fun headlineLine(i: WeeklyInsights): String {
    val sleep = formatDuration(i.avgSleepMinThisWeek)
    return "Avg sleep $sleep · ${i.stepsGoalHitDays}/7 step goals"
}
