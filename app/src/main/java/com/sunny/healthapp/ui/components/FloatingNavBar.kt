package com.sunny.healthapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class NavItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val accent: Color,
)

@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    selectedKey: String?,
    onSelect: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier = modifier.padding(
            start = 22.dp,
            end = 22.dp,
            bottom = bottomInset + 18.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(36.dp))
                .background(
                    Brush.linearGradient(
                        0.0f to Color.White.copy(alpha = 0.10f),
                        1.0f to Color.White.copy(alpha = 0.04f),
                    )
                )
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
                .border(
                    width = 0.7.dp,
                    brush = Brush.linearGradient(
                        0.0f to Color.White.copy(alpha = 0.30f),
                        1.0f to Color.White.copy(alpha = 0.04f),
                    ),
                    shape = RoundedCornerShape(36.dp),
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                NavPill(
                    item = item,
                    selected = item.key == selectedKey,
                    onClick = { onSelect(item) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavPill(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillBg by animateColorAsState(
        targetValue = if (selected) item.accent.copy(alpha = 0.18f) else Color.Transparent,
        animationSpec = tween(280),
        label = "navPillBg",
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) item.accent else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(280),
        label = "navIcon",
    )
    val height by animateDpAsState(
        targetValue = if (selected) 52.dp else 48.dp,
        animationSpec = tween(280),
        label = "navHeight",
    )

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(28.dp))
            .background(pillBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier.size(22.dp),
            )
            if (selected) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = item.accent,
                )
            }
        }
    }
}
