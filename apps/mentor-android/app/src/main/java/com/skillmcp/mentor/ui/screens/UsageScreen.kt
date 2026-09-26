package com.skillmcp.mentor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.UsageWindow
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.GlassCard
import com.skillmcp.mentor.ui.components.ScreenHeader
import com.skillmcp.mentor.mentor.ScreenSuggestions
import com.skillmcp.mentor.mentor.SuggestionScreen
import com.skillmcp.mentor.ui.components.StatCard
import com.skillmcp.mentor.ui.components.TabSuggestions
import java.util.Locale

@Composable
fun UsageScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    val totals = state.usageTotals

    AppBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ScreenHeader(
                    title = "Usage",
                    subtitle = "Track requests, tokens, and estimated spend across your models.",
                )
            }
            item {
                TabSuggestions(
                    title = "Ask about usage",
                    suggestions = ScreenSuggestions.forScreen(SuggestionScreen.USAGE),
                    onSelect = vm::openChatWithSuggestion,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UsageWindow.entries.forEach { window ->
                        FilterChip(
                            selected = state.usageWindow == window,
                            onClick = { vm.setUsageWindow(window) },
                            label = { Text(window.label) },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(title = "Requests", value = totals.requestCount.toString(), modifier = Modifier.weight(1f))
                    StatCard(
                        title = "Tokens",
                        value = formatTokens(totals.promptTokens + totals.completionTokens),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                StatCard(
                    title = "Estimated spend",
                    value = "$${String.format(Locale.US, "%.4f", totals.estimatedUsd)}",
                    subtitle = "Based on rates in Models",
                )
            }
            item {
                Text("By model", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (state.usageByModel.isEmpty()) {
                item {
                    GlassCard {
                        Text(
                            "No usage in this period yet. Start a conversation in Chat.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(state.usageByModel) { row ->
                GlassCard {
                    Text("${row.providerName} · ${row.model}", fontWeight = FontWeight.Medium)
                    Text(
                        "${row.requestCount} requests · ${formatTokens(row.totalTokens)} tokens",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "$${String.format(Locale.US, "%.4f", row.estimatedUsd)}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            item {
                Text("Daily", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            }
            items(state.usageByDay) { day ->
                GlassCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(day.dayKey)
                        Text("${day.requestCount} req · $${String.format(Locale.US, "%.3f", day.estimatedUsd)}")
                    }
                }
            }
        }
    }
}

private fun formatTokens(n: Long): String =
    when {
        n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
        n >= 1_000 -> String.format(Locale.US, "%.1fK", n / 1_000.0)
        else -> n.toString()
    }
