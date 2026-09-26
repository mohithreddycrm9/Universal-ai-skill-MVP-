package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.mentor.QuickSuggestion

@Composable
fun TabSuggestions(
    title: String,
    suggestions: List<QuickSuggestion>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    chipsPerRow: Int = 7,
) {
    if (suggestions.isEmpty()) return
    Column(modifier = modifier.padding(bottom = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        suggestions.chunked(chipsPerRow).forEachIndexed { index, row ->
            SuggestionChipRow(
                labels = row.map { quick -> quick.label to { onSelect(quick.prompt) } },
                modifier = if (index > 0) Modifier.padding(top = 6.dp) else Modifier,
            )
        }
    }
}
