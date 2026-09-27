package com.skillmcp.mentor.ui.components.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
                contentDescription = null,
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(0.dp),
            userScrollEnabled = false,
        ) {
            items(starters.take(4)) { card ->
                StarterGridCard(card)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StarterGridCard(card: ChatStarterCard) {
    Surface(
        modifier = Modifier.fillMaxWidth().pressableScale(),
        onClick = card.onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(card.icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
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
    val who = if (name.isBlank()) "there" else name
    return "$period, $who 👋"
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
