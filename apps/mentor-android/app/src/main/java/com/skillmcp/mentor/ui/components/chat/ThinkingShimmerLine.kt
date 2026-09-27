package com.skillmcp.mentor.ui.components.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.R
import com.skillmcp.mentor.ui.motion.rememberReduceMotion

/** Base opacity keeps the label readable (WCAG-friendly) even at the dimmest point of the sweep. */
const val THINKING_BASE_ALPHA = 0.7f

/**
 * "Thinking…" with a moving highlight (gradient shimmer). With reduced motion it is static text at
 * [THINKING_BASE_ALPHA]. Announced politely to TalkBack.
 */
@Composable
fun ThinkingShimmerLine(modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = THINKING_BASE_ALPHA)
    val highlight = MaterialTheme.colorScheme.onSurface
    val textModifier =
        modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
    val label = stringResource(R.string.chat_thinking)
    if (reduceMotion) {
        Text(text = label, modifier = textModifier, style = MaterialTheme.typography.bodyLarge, color = base)
        return
    }
    val transition = rememberInfiniteTransition(label = "thinking")
    val sweep by transition.animateFloat(
        initialValue = -SWEEP_WIDTH_PX,
        targetValue = SWEEP_TRAVEL_PX,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1_400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "thinkingSweep",
    )
    Text(
        text = label,
        modifier = textModifier,
        style =
            MaterialTheme.typography.bodyLarge.copy(
                brush =
                    Brush.linearGradient(
                        colors = listOf(base, highlight, base),
                        start = Offset(sweep, 0f),
                        end = Offset(sweep + SWEEP_WIDTH_PX, 0f),
                    ),
            ),
    )
}

private const val SWEEP_WIDTH_PX = 160f
private const val SWEEP_TRAVEL_PX = 420f
