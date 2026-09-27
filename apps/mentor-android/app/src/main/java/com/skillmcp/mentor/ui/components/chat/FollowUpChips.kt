package com.skillmcp.mentor.ui.components.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.R
import com.skillmcp.mentor.mentor.FollowUpKind
import com.skillmcp.mentor.ui.theme.PillShape

/** Suggested follow-ups under the latest reply; tapping one sends it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FollowUpChips(
    kinds: List<FollowUpKind>,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (kinds.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        kinds.forEach { kind ->
            val label = kind.label()
            val prompt = kind.prompt()
            Surface(
                onClick = { onSend(prompt) },
                shape = PillShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun FollowUpKind.label(): String =
    stringResource(
        when (this) {
            FollowUpKind.SHORTER -> R.string.followup_shorter
            FollowUpKind.EXAMPLE -> R.string.followup_example
            FollowUpKind.SIMPLER -> R.string.followup_simpler
            FollowUpKind.NEXT_STEPS -> R.string.followup_next_steps
            FollowUpKind.TABLE -> R.string.followup_table
            FollowUpKind.CHECKLIST -> R.string.followup_checklist
            FollowUpKind.EXPLAIN_CODE -> R.string.followup_explain_code
            FollowUpKind.ADD_TESTS -> R.string.followup_add_tests
        },
    )

@Composable
private fun FollowUpKind.prompt(): String =
    stringResource(
        when (this) {
            FollowUpKind.SHORTER -> R.string.followup_shorter_prompt
            FollowUpKind.EXAMPLE -> R.string.followup_example_prompt
            FollowUpKind.SIMPLER -> R.string.followup_simpler_prompt
            FollowUpKind.NEXT_STEPS -> R.string.followup_next_steps_prompt
            FollowUpKind.TABLE -> R.string.followup_table_prompt
            FollowUpKind.CHECKLIST -> R.string.followup_checklist_prompt
            FollowUpKind.EXPLAIN_CODE -> R.string.followup_explain_code_prompt
            FollowUpKind.ADD_TESTS -> R.string.followup_add_tests_prompt
        },
    )
