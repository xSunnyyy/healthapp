package com.sunny.healthapp.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Drop-in replacement for [Modifier.clickable] that fires a haptic confirm
 * before the click body. Use this on every tap target the user expects to
 * feel pressed: scrubber arrows, period tabs, tile taps, info icons.
 */
@Composable
fun Modifier.hapticClickable(
    enabled: Boolean = true,
    haptic: HapticFeedbackType = HapticFeedbackType.LongPress,
    onClick: () -> Unit,
): Modifier = composed {
    val feedback = LocalHapticFeedback.current
    clickable(enabled = enabled) {
        feedback.performHapticFeedback(haptic)
        onClick()
    }
}

/**
 * Just trigger a haptic. Useful in non-button contexts (e.g. animation completion).
 */
@Composable
fun rememberHapticTrigger(): (HapticFeedbackType) -> Unit {
    val feedback = LocalHapticFeedback.current
    return { type -> feedback.performHapticFeedback(type) }
}
