package com.skillmcp.mentor.ui.components.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.mentor.PopularUseCase
import com.skillmcp.mentor.ui.components.motion.pressableScale
import com.skillmcp.mentor.ui.theme.BrandColors

@Composable
fun DiscoverCategorySections(
    categories: List<String>,
    useCases: List<PopularUseCase>,
    onUseCase: (PopularUseCase) -> Unit,
    iconForCategory: (String) -> ImageVector,
) {
    categories.forEach { category ->
        val items = useCases.filter { it.category == category }
        if (items.isEmpty()) return@forEach
        Text(
            category,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 8.dp),
        ) {
            items(items.take(6), key = { it.id }) { useCase ->
                WorkflowImageCard(
                    useCase = useCase,
                    icon = iconForCategory(category),
                    onClick = { onUseCase(useCase) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkflowImageCard(
    useCase: PopularUseCase,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(160.dp).height(120.dp).pressableScale(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = BrandColors.Indigo.copy(alpha = 0.15f),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(useCase.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(
                useCase.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
