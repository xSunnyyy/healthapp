package com.sunny.healthapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunny.healthapp.ui.theme.EdgeFaint
import com.sunny.healthapp.ui.theme.Ink800
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary

/**
 * Clean dark stat card. No gradient fill — just a single ink surface with a
 * hairline edge. Layout: icon + label (top), big value + unit, accent-tinted
 * status, then any small chart (composable slot) pinned to the bottom.
 */
@Composable
fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String? = null,
    status: String? = null,
    accent: Color,
    chart: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Ink800)
            .border(0.7.dp, EdgeFaint, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
        ) {
            // Icon + label on one line
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 0.3.sp,
                    ),
                    color = TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(10.dp))
            // Value + unit
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
                if (unit != null) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            // Status, in accent color
            if (status != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = accent,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.weight(1f))
            if (chart != null) chart()
        }
    }
}

@Composable
fun StatTileRow(
    tiles: List<@Composable (Modifier) -> Unit>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tiles.forEach { tile -> tile(Modifier.weight(1f).height(160.dp)) }
    }
}

