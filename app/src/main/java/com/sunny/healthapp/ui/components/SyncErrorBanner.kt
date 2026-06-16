package com.sunny.healthapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.ui.theme.Crimson
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary

@Composable
fun SyncErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Crimson.copy(alpha = 0.12f))
            .border(0.7.dp, Crimson.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = "Sync error",
            tint = Crimson,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 1.dp),
        )
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                text = "Sync issue",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}
