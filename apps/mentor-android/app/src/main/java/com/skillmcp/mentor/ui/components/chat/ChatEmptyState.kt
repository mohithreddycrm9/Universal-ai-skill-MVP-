package com.skillmcp.mentor.ui.components.chat

import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.R
import com.skillmcp.mentor.mentor.PopularUseCase
import com.skillmcp.mentor.ui.components.motion.pressableScale
import com.skillmcp.mentor.ui.motion.rememberReduceMotion
import com.skillmcp.mentor.ui.theme.BrandColors
import com.skillmcp.mentor.ui.theme.CategoryPalette
import com.skillmcp.mentor.ui.theme.MentorDimens
import com.skillmcp.mentor.ui.theme.accent
import java.util.Calendar

data class ChatStarterCard(
    val title: String,
    val hint: String,
    val icon: ImageVector,
    val accent: @Composable () -> Color,
    val onClick: () -> Unit,
)

enum class DayPart { MORNING, AFTERNOON, EVENING }

/** Hour override for the greeting (snapshot tests pin it so renders don't depend on the clock). */
val LocalGreetingHour = staticCompositionLocalOf<Int?> { null }

/** 05:00–11:59 morning, 12:00–16:59 afternoon, otherwise evening (late night reads as evening). */
fun dayPartFor(hourOfDay: Int): DayPart =
    when (hourOfDay) {
        in 5..11 -> DayPart.MORNING
        in 12..16 -> DayPart.AFTERNOON
        else -> DayPart.EVENING
    }

/** First name only (max 40 chars) so long full names don't wrap the greeting; blank -> null. */
fun greetingName(displayName: String): String? =
    displayName.trim().split(Regex("\\s+")).firstOrNull()?.take(40)?.takeIf { it.isNotBlank() }

@StringRes
fun greetingRes(part: DayPart, hasName: Boolean): Int =
    when (part) {
        DayPart.MORNING -> if (hasName) R.string.greeting_morning_name else R.string.greeting_morning
        DayPart.AFTERNOON -> if (hasName) R.string.greeting_afternoon_name else R.string.greeting_afternoon
        DayPart.EVENING -> if (hasName) R.string.greeting_evening_name else R.string.greeting_evening
    }

@Composable
fun ChatEmptyState(
    displayName: String,
    starters: List<ChatStarterCard>,
    modifier: Modifier = Modifier,
    hourOfDay: Int = LocalGreetingHour.current ?: remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) },
) {
    val name = greetingName(displayName)
    val res = greetingRes(dayPartFor(hourOfDay), name != null)
    val greeting = if (name != null) stringResource(res, name) else stringResource(res)
    val reduceMotion = rememberReduceMotion()
    val scale =
        if (reduceMotion) {
            1f
        } else {
            val pulse = rememberInfiniteTransition(label = "mark")
            val animated by pulse.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(animation = tween(2600), repeatMode = RepeatMode.Reverse),
                label = "markScale",
            )
            animated
        }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandMark(Modifier.scale(scale))
        Spacer(Modifier.height(20.dp))
        Text(
            greeting,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.empty_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (starters.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.empty_try_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp, start = 2.dp),
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = starterColumns(maxWidth.value, effectiveTextScale())
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    starters.take(4).chunked(columns).forEach { rowCards ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowCards.forEach { card -> StarterCard(card, Modifier.weight(1f)) }
                            if (rowCards.size < columns) Spacer(Modifier.weight((columns - rowCards.size).toFloat()))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Box(modifier.size(88.dp), contentAlignment = Alignment.Center) {
        // Soft halo behind the mark.
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(BrandColors.Indigo.copy(alpha = 0.22f), BrandColors.Coral.copy(alpha = 0.10f), Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(BrandColors.Indigo, BrandColors.Coral))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun StarterCard(
    card: ChatStarterCard,
    modifier: Modifier = Modifier,
) {
    val accent = card.accent()
    Surface(
        onClick = card.onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 112.dp).pressableScale(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(card.icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Text(
                card.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                card.hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
            )
        }
    }
}

fun popularUseCasesToStarters(
    useCases: List<PopularUseCase>,
    onUseCase: (PopularUseCase) -> Unit,
): List<ChatStarterCard> =
    useCases.take(4).map { uc ->
        val style = CategoryPalette.styleFor(uc.category)
        ChatStarterCard(
            title = uc.title,
            hint = uc.subtitle,
            icon = style.icon,
            accent = { style.accent() },
            onClick = { onUseCase(uc) },
        )
    }

/** System font scale times the in-app type multiplier (bodyLarge is 16sp at 1x). */
@Composable
private fun effectiveTextScale(): Float =
    LocalDensity.current.fontScale * (MaterialTheme.typography.bodyLarge.fontSize.value / 16f)

/** Two starter cards per row normally; one per row at large text or on narrow widths. */
fun starterColumns(widthDp: Float, textScale: Float): Int =
    if (textScale >= 1.3f || widthDp / textScale < 320f) 1 else 2
