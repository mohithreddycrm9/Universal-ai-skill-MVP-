package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SuggestionChipRow(
    labels: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val surface = MaterialTheme.colorScheme.surface
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll)
                    .padding(top = 4.dp, bottom = 4.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            labels.forEach { (label, onClick) ->
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
        }
        if (scroll.canScrollForward) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .width(32.dp)
                        .height(52.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, surface),
                            ),
                        ),
            )
        }
    }
}
