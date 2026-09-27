package com.skillmcp.mentor.ui.components.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.ui.theme.BrandColors

@Composable
fun VoiceModeOverlay(
    transcript: String,
    listening: Boolean,
    onDismiss: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "voiceOrb")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(900),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier =
                    Modifier
                        .size(160.dp)
                        .scale(if (listening) pulse else 1f)
                        .background(
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        BrandColors.Indigo.copy(alpha = 0.85f),
                                        BrandColors.Coral.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.background,
                                    ),
                            ),
                            CircleShape,
                        ),
            )
            Text(
                if (listening) "Listening…" else "Voice",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 24.dp),
            )
            if (transcript.isNotBlank()) {
                Text(
                    transcript,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                )
            }
            TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                Text("Close")
            }
        }
    }
}
