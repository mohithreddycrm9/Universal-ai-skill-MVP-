package com.skillmcp.mentor.ui.components.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import com.skillmcp.mentor.ui.theme.BrandColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.mentor.PopularUseCase
import com.skillmcp.mentor.ui.components.motion.pressableScale
import com.skillmcp.mentor.ui.theme.MentorDimens
import java.util.Calendar

data class ChatStarterCard(
    val title: String,
    val hint: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun ChatEmptyState(
    displayName: String,
    starters: List<ChatStarterCard>,
    modifier: Modifier = Modifier,
) {
    val greeting = timeOfDayGreeting(displayName)
    val pulse = rememberInfiniteTransition(label = "orb")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(animation = tween(2400), repeatMode = RepeatMode.Reverse),
        label = "orbScale",
    )
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = MentorDimens.ScreenHorizontal, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(120.dp).scale(scale),
            shape = MaterialTheme.shapes.extraLarge,
            color = Color.Transparent,
            tonalElevation = 0.dp,
        ) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxWidth()) {
                drawCircle(
                    brush =
                        Brush.radialGradient(
                            colors = listOf(BrandColors.Indigo, BrandColors.IndigoDark, BrandColors.Coral.copy(0.35f)),
                        ),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            greeting,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            "Pick a workflow or type below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            starters.take(4).chunked(2).forEach { rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowCards.forEach { card ->
                        StarterGridCard(card, modifier = Modifier.weight(1f))
                    }
                    if (rowCards.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StarterGridCard(
    card: ChatStarterCard,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().pressableScale(),
        onClick = card.onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(card.icon, contentDescription = card.title, tint = MaterialTheme.colorScheme.secondary)
            Text(card.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                card.hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

fun timeOfDayGreeting(name: String): String {
    val who = if (name.isBlank()) "there" else name.trim()
    return "Hi $who, what can I help with?"
}

fun popularUseCasesToStarters(
    useCases: List<PopularUseCase>,
    onUseCase: (PopularUseCase) -> Unit,
): List<ChatStarterCard> {
    val icons =
        listOf(
            Icons.Outlined.Lightbulb,
            Icons.Outlined.WorkOutline,
            Icons.Outlined.Restaurant,
            Icons.Outlined.AutoAwesome,
        )
    return useCases.take(4).mapIndexed { index, uc ->
        ChatStarterCard(
            title = uc.title,
            hint = uc.subtitle,
            icon = icons.getOrElse(index) { Icons.Outlined.AutoAwesome },
            onClick = { onUseCase(uc) },
        )
    }
}
