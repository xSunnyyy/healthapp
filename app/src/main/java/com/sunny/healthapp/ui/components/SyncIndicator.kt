package com.sunny.healthapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.data.sync.SyncStatus
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.EdgeSoft
import com.sunny.healthapp.ui.theme.Ink800
import com.sunny.healthapp.ui.theme.MintGlow
import com.sunny.healthapp.ui.theme.TextSecondary

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

    val (icon, tint) = when (status) {
        is SyncStatus.Syncing -> Icons.Outlined.Sync to Accent
        is SyncStatus.Done -> Icons.Outlined.Check to MintGlow
        is SyncStatus.Error -> Icons.Outlined.ErrorOutline to Crimson
        SyncStatus.Idle -> Icons.Outlined.Sync to TextSecondary
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Ink800.copy(alpha = 0.7f))
            .border(0.6.dp, EdgeSoft, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Sync",
            tint = tint,
            modifier = Modifier
                .size(16.dp)
                .rotate(rotation),
        )
        // Only show the label while actively syncing — otherwise just an icon pill.
        AnimatedVisibility(
            visible = isSyncing,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
        ) {
            val msg = (status as? SyncStatus.Syncing)?.message ?: "Syncing"
            Text(
                text = msg,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
            )
        }
    }
}

@Composable
fun SyncDot(
    status: SyncStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 32.dp,
) {
    // Compact circular sync button. While Syncing the icon spins continuously.
    val (icon, tint) = when (status) {
        is SyncStatus.Syncing -> Icons.Outlined.Sync to Accent
        is SyncStatus.Done -> Icons.Outlined.Check to MintGlow
        is SyncStatus.Error -> Icons.Outlined.ErrorOutline to Crimson
        SyncStatus.Idle -> Icons.Outlined.Sync to TextSecondary
    }
    val rotation = if (status is SyncStatus.Syncing) {
        val transition = rememberInfiniteTransition(label = "dotSpin")
        val v by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "dotSpinV",
        )
        v
    } else 0f
    val iconSize = (size.value * 0.42f).dp.coerceAtLeast(12.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Ink800.copy(alpha = 0.55f))
            .border(0.6.dp, EdgeSoft, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Sync",
            tint = tint,
            modifier = Modifier.size(iconSize).rotate(rotation),
        )
    }
}
