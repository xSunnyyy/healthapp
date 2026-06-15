package com.sunny.healthapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.ui.theme.Accent

@Composable
fun TodayChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Accent.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Today,
            contentDescription = "Return to today",
            tint = Accent,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = "Today",
            style = MaterialTheme.typography.labelMedium,
            color = Accent,
        )
    }
}

/**
 * Wraps content in a Material3 PullToRefreshBox so a user pulling down from
 * the top of any scroll surface triggers [onRefresh]. The default progress
 * indicator is suppressed — we want the SyncDot in the header to be the
 * single source of visual feedback while syncing, no extra dot at the top.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableContent(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        indicator = { /* intentionally empty — feedback lives in the SyncDot */ },
        content = content,
    )
}
