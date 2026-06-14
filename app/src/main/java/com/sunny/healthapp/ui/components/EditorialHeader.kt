package com.sunny.healthapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary

@Composable
fun EditorialHeader(
    eyebrow: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 22.dp)) {
        if (eyebrow != null) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary,
        )
    }
}
