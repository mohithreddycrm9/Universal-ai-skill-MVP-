package com.skillmcp.mentor.ui.components.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.R
import com.skillmcp.mentor.mentor.PopularUseCase
import com.skillmcp.mentor.ui.theme.BrandColors
import com.skillmcp.mentor.ui.theme.CategoryPalette

private val HeroDeep = Color(0xFF3730A3)

/** Gradient hero for the ranked workflow. Text is white on indigo (>= 7:1); nothing renders without a workflow. */
@Composable
fun DiscoverHeroCard(
    workflow: PopularUseCase,
    onTry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = CategoryPalette.styleFor(workflow.category)
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(BrandColors.Indigo, HeroDeep))),
    ) {
        // Oversized, decorative category glyph bleeding off the corner.
        Icon(
            style.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.12f),
            modifier = Modifier.align(Alignment.TopEnd).size(150.dp).offset(x = 28.dp, y = (-18).dp),
        )
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.discover_workflow_of_day).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
            )
            Text(
                workflow.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                workflow.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
            )
            Button(
                onClick = onTry,
                modifier = Modifier.padding(top = 6.dp).heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandColors.Indigo),
                shape = RoundedCornerShape(50),
            ) {
                Text(stringResource(R.string.discover_start), fontWeight = FontWeight.SemiBold)
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 6.dp).size(18.dp),
                )
            }
        }
    }
}

@Composable
fun DiscoverSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder ?: stringResource(R.string.discover_placeholder)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.drawer_clear_search))
                }
            }
        },
        shape = RoundedCornerShape(50),
        singleLine = true,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
    )
}
