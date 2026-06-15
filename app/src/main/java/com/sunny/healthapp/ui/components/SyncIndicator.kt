package com.sunny.healthapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.data.sync.SyncStatus
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.EdgeSoft
import com.sunny.healthapp.ui.theme.Ink800
import com.sunny.healthapp.ui.theme.MintGlow
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextSecondary
import java.time.Duration
import java.time.Instant

@Composable
fun SyncIndicator(
    status: SyncStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSyncing = status is SyncStatus.Syncing

    val rotation = if (isSyncing) {
        val transition = rememberInfiniteTransition(label = "syncSpin")
        val v by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "syncSpinV",
        )
        v
    } else 0f

    val (icon, tint, label) = when (status) {
        is SyncStatus.Syncing -> Triple(Icons.Outlined.Sync, Accent, status.message)
        is SyncStatus.Done -> Triple(
            Icons.Outlined.Check,
            MintGlow,
            "Synced ${relative(status.at)}",
        )
        is SyncStatus.Error -> Triple(Icons.Outlined.ErrorOutline, Crimson, "Sync failed")
        SyncStatus.Idle -> Triple(Icons.Outlined.Sync, TextSecondary, "Tap to sync")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Ink800.copy(alpha = 0.7f))
            .border(0.6.dp, EdgeSoft, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier
                .size(16.dp)
                .rotate(rotation),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}

private fun relative(at: Instant): String {
    val mins = Duration.between(at, Instant.now()).toMinutes()
    return when {
        mins < 1 -> "now"
        mins < 60 -> "${mins}m ago"
        mins < 24 * 60 -> "${mins / 60}h ago"
        else -> "${mins / 1440}d ago"
    }
}
