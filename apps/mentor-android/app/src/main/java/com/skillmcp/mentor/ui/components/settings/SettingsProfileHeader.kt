package com.skillmcp.mentor.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.llm.LlmProfile
import com.skillmcp.mentor.llm.UsageTotals
import com.skillmcp.mentor.ui.theme.BrandColors
import com.skillmcp.mentor.ui.theme.MentorDimens

@Composable
fun SettingsProfileHeader(
    displayName: String,
    profile: LlmProfile?,
    monthSpendUsd: Double,
    monthlyBudgetUsd: Double,
    modifier: Modifier = Modifier,
) {
    val name = displayName.ifBlank { "You" }
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    val budgetProgress =
        if (monthlyBudgetUsd > 0) {
            (monthSpendUsd / monthlyBudgetUsd).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
    val ringColor =
        when {
            monthlyBudgetUsd <= 0 -> MaterialTheme.colorScheme.primary
            budgetProgress >= 1f -> MaterialTheme.colorScheme.error
            budgetProgress >= 0.8f -> BrandColors.Coral
            else -> MaterialTheme.colorScheme.primary
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    initial,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                profile?.let {
                    Text(
                        "${it.name} · ${it.model}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    progress = { if (monthlyBudgetUsd > 0) budgetProgress else 0.35f },
                    modifier = Modifier.size(52.dp),
                    color = ringColor,
                    strokeWidth = 4.dp,
                )
                Text(
                    "$${"%.2f".format(monthSpendUsd)}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "this month",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
