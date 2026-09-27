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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.ui.motion.rememberReduceMotion

@Composable
fun ThinkingShimmerLine(modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val alpha =
        if (reduceMotion) {
            0.6f
        } else {
            val transition = rememberInfiniteTransition(label = "thinking")
            val animated by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 0.95f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(900, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "thinkingAlpha",
            )
            animated
        }
    Text(
        text = "Thinking…",
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp).alpha(alpha),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
