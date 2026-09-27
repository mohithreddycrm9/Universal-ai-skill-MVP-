package com.skillmcp.mentor.ui.motion

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object CalmMotion {
    const val FAST_MS = 200
    const val SCREEN_MS = 320

    const val DAMPING = 0.85f
    const val STIFFNESS = Spring.StiffnessMediumLow
    const val PRESS_SCALE = 0.97f

    /** The one calm spring (damping 0.85, MediumLow) for any value type. */
    fun <T> gentle(): SpringSpec<T> = spring(dampingRatio = DAMPING, stiffness = STIFFNESS)

    val gentleSpring: SpringSpec<Float> = gentle()

    fun <T> fastTween(reduceMotion: Boolean): FiniteAnimationSpec<T> =
        if (reduceMotion) {
            tween(durationMillis = 0)
        } else {
            tween(durationMillis = FAST_MS)
        }
}

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { isReduceMotionEnabled(context) }
}

fun isReduceMotionEnabled(context: Context): Boolean {
    return Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) <= 0f
}
