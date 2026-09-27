package com.skillmcp.mentor.ui.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

@Composable
fun rememberHapticView(): View = LocalView.current

/** CONFIRM exists from Android 11 (API 30); older devices get the closest short tick. */
fun View.performSendHaptic() {
    performHapticFeedback(sendHapticConstant(Build.VERSION.SDK_INT))
}

internal fun sendHapticConstant(sdkInt: Int): Int =
    if (sdkInt >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
    } else {
        HapticFeedbackConstants.VIRTUAL_KEY
    }

fun View.performLightTap() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

fun View.performLongPressHaptic() {
    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}
