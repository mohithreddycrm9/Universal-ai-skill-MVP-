package com.skillmcp.mentor.ui.util

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

@Composable
fun rememberHapticView(): View = LocalView.current

fun View.performSendHaptic() {
    performHapticFeedback(HapticFeedbackConstants.CONFIRM)
}

fun View.performLightTap() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

fun View.performLongPressHaptic() {
    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}
