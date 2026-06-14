package com.sunny.healthapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.ui.theme.Ink700
import com.sunny.healthapp.ui.theme.Ink900

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
            start = 28.dp,
            end = 28.dp,
            bottom = bottomInset + 16.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(40.dp))
                .background(
                    Brush.linearGradient(
                        0.0f to Ink700.copy(alpha = 0.92f),
                        1.0f to Ink900.copy(alpha = 0.92f),
                    )
                )
                .border(
                    width = 0.7.dp,
                    brush = Brush.linearGradient(
                        0.0f to Color.White.copy(alpha = 0.20f),
                        1.0f to Color.White.copy(alpha = 0.04f),
                    ),
                    shape = RoundedCornerShape(40.dp),
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                NavCircle(
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
private fun NavCircle(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val circleBg by animateColorAsState(
        targetValue = if (selected) Color.White else Color.Transparent,
        animationSpec = tween(260),
        label = "navCircleBg",
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) Color.Black else Color.White.copy(alpha = 0.75f),
        animationSpec = tween(260),
        label = "navIcon",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(circleBg)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
