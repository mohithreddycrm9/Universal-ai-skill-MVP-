package com.skillmcp.mentor.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.llm.ModelPreset
import com.skillmcp.mentor.ui.theme.PillShape

@Composable
fun PresetSegmentedControl(
    selected: ModelPreset,
    onSelect: (ModelPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ModelPreset.entries.forEach { preset ->
            val isSelected = preset == selected
            Text(
                text = preset.label,
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        )
                        .clickable { onSelect(preset) }
                        .padding(vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
