package com.skillmcp.mentor.ui.components.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.skillmcp.mentor.ui.motion.CalmMotion
import com.skillmcp.mentor.ui.motion.isReduceMotionEnabled

/**
 * Scales to [CalmMotion.PRESS_SCALE] while pressed using the calm spring. Observes pointers on the
 * Initial pass without consuming them, so the wrapped clickable still receives the tap.
 * Disabled when the system animator scale is 0 (reduced motion).
 */
fun Modifier.pressableScale(enabled: Boolean = true): Modifier =
    composed {
        val context = LocalContext.current
        val reduceMotion = remember(context) { isReduceMotionEnabled(context) }
        if (!enabled || reduceMotion) return@composed this
        var pressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (pressed) CalmMotion.PRESS_SCALE else 1f,
            animationSpec = CalmMotion.gentleSpring,
            label = "pressScale",
        )
        this
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    pressed = true
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    pressed = false
                }
            }
    }
