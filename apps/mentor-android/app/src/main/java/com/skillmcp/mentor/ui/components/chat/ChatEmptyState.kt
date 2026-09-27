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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = MentorDimens.ScreenHorizontal, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = "Assistant",
                modifier = Modifier.padding(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(16.dp))
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
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val period =
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Hello"
        }
    return if (name.isBlank()) "$period 👋" else "$period, $name 👋"
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
