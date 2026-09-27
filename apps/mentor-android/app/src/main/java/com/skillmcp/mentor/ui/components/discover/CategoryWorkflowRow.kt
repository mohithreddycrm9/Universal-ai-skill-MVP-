package com.skillmcp.mentor.ui.components.discover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.mentor.PopularUseCase
import com.skillmcp.mentor.ui.components.motion.pressableScale
import com.skillmcp.mentor.ui.theme.CategoryPalette
import com.skillmcp.mentor.ui.theme.CategoryStyle
import com.skillmcp.mentor.ui.theme.accent

@Composable
fun CategoryStyle.displayName(category: String): String = label?.let { stringResource(it) } ?: category

/** One horizontally scrolling row of visual cards per category. */
@Composable
fun DiscoverCategorySections(
    categories: List<String>,
    useCases: List<PopularUseCase>,
    onUseCase: (PopularUseCase) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        categories.forEach { category ->
            val items = useCases.filter { it.category == category }
            if (items.isEmpty()) return@forEach
            val style = CategoryPalette.styleFor(category)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(style.icon, contentDescription = null, tint = style.accent(), modifier = Modifier.size(20.dp))
                    Text(
                        style.displayName(category),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp).semantics { heading() },
                    )
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    items(items.take(6), key = { it.id }) { useCase ->
                        WorkflowVisualCard(useCase = useCase, onClick = { onUseCase(useCase) }, modifier = Modifier.width(196.dp))
                    }
                }
            }
        }
    }
}

/** Visual card: a tinted art band (gradient + oversized glyph) over the title and hint. */
@Composable
fun WorkflowVisualCard(
    useCase: PopularUseCase,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = CategoryPalette.styleFor(useCase.category)
    val accent = style.accent()
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 176.dp).pressableScale(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.06f)))),
            ) {
                Icon(
                    style.icon,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.18f),
                    modifier = Modifier.align(Alignment.BottomEnd).size(84.dp).offset(x = 14.dp, y = 18.dp),
                )
                Box(
                    Modifier
                        .padding(14.dp)
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(style.icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(useCase.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(
                    useCase.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                )
            }
        }
    }
}
