package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.mentor.QuickSuggestion

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TabSuggestions(
    title: String,
    suggestions: List<QuickSuggestion>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxVisible: Int = 8,
) {
    if (suggestions.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val visible = if (expanded) suggestions else suggestions.take(maxVisible)
    Column(modifier = modifier.padding(bottom = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            visible.forEach { quick ->
                InputChip(
                    selected = false,
                    onClick = { onSelect(quick.prompt) },
                    label = { Text(quick.label, maxLines = 2) },
                    colors =
                        InputChipDefaults.inputChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                        ),
                )
            }
            if (!expanded && suggestions.size > maxVisible) {
                InputChip(
                    selected = false,
                    onClick = { expanded = true },
                    label = { Text("More…") },
                )
            }
        }
    }
}
