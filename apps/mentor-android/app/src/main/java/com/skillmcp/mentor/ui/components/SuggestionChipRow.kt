package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionChipRow(
    labels: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val visible = if (expanded) labels else labels.take(4)

    FlowRow(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        visible.forEach { (label, onClick) ->
            InputChip(
                selected = false,
                onClick = onClick,
                label = { Text(label, maxLines = 2) },
                colors =
                    InputChipDefaults.inputChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                        labelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                border = InputChipDefaults.inputChipBorder(enabled = true, selected = false),
            )
        }
        if (!expanded && labels.size > 4) {
            InputChip(
                selected = false,
                onClick = { expanded = true },
                label = { Text("More…") },
            )
        }
    }
}
