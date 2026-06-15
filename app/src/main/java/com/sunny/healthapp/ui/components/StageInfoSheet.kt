package com.sunny.healthapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.domain.model.SleepStage
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.AccentDeep
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.Ink800
import com.sunny.healthapp.ui.theme.Lavender
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary

private data class StageInfo(
    val title: String,
    val color: Color,
    val typical: String,
    val why: String,
    val tips: String,
)

private fun infoFor(stage: SleepStage): StageInfo = when (stage) {
    SleepStage.Deep -> StageInfo(
        title = "Deep sleep",
        color = AccentDeep,
        typical = "Typical: 13–23% of the night, ~1–2 hours.",
        why = "The most physically restorative stage. Heart rate and " +
              "breathing slow to their lowest, the body releases growth hormone, " +
              "tissue and muscle repair happens, and the immune system rebuilds.",
        tips = "Boosted by: consistent sleep schedule, cool dark room, and " +
              "avoiding alcohol within 3 hours of bed.",
    )
    SleepStage.REM -> StageInfo(
        title = "REM sleep",
        color = Lavender,
        typical = "Typical: 20–25% of the night, ~1.5–2 hours.",
        why = "Brain-active sleep where most dreams happen. Crucial for " +
              "memory consolidation, learning, emotional regulation, and " +
              "creativity. Comes in cycles roughly every 90 minutes, with " +
              "longer REM blocks toward morning.",
        tips = "Boosted by: getting enough total sleep, regular bedtime, and " +
              "limiting caffeine after lunch.",
    )
    SleepStage.Light -> StageInfo(
        title = "Light sleep",
        color = Accent,
        typical = "Typical: 50–60% of the night — the largest single chunk.",
        why = "The transition layer between waking and deeper sleep. The body " +
              "still does restorative work here (memory consolidation, lower " +
              "heart rate, muscle relaxation), and brief micro-wakings during " +
              "Light are completely normal.",
        tips = "It's not 'bad' sleep — your body needs a lot of it. Worry " +
              "only if Deep and REM are unusually low.",
    )
    SleepStage.Awake -> StageInfo(
        title = "Awake / restless",
        color = Crimson,
        typical = "Typical: a few minutes scattered across the night.",
        why = "Brief wake periods are a normal part of healthy sleep — almost " +
              "everyone wakes briefly between cycles and just doesn't remember. " +
              "Long stretches of awake time mid-night can point to stress, " +
              "alcohol, late caffeine, or a room that's too warm.",
        tips = "Reduce by: dimming lights ~1h before bed, keeping the room " +
              "cool (~18°C / 65°F), and avoiding screens in bed.",
    )
    SleepStage.Unknown -> StageInfo(
        title = "Stage",
        color = TextSecondary,
        typical = "",
        why = "No additional information available.",
        tips = "",
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepStageInfoSheet(
    stage: SleepStage,
    onDismiss: () -> Unit,
) {
    val info = infoFor(stage)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Ink800,
        contentColor = TextPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(TextSecondary.copy(alpha = 0.4f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(info.color),
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    info.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
            }
            if (info.typical.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    info.typical,
                    style = MaterialTheme.typography.labelMedium,
                    color = info.color,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Why it matters",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                info.why,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            if (info.tips.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "How to support it",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    info.tips,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
