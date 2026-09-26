package com.skillmcp.mentor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import com.skillmcp.mentor.ui.components.StatCard
import java.util.Locale

@Composable
fun UsageScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    val totals = state.usageTotals

    AppBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Usage", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Token and estimated cost tracking across your connected models.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UsageWindow.entries.forEach { window ->
                        FilterChip(
                            selected = state.usageWindow == window,
                            onClick = { vm.setUsageWindow(window) },
                            label = { Text(window.label) },
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title = "Requests",
                        value = totals.requestCount.toString(),
                        modifier = Modifier.weight(1f),
                    )
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
                    subtitle = "Based on per-model rates you configure",
                )
            }
            item {
                Text("By model", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            }
            if (state.usageByModel.isEmpty()) {
                item {
                    Text(
                        "No usage in this period yet. Send a message in Chat to start tracking.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.usageByModel) { row ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${row.providerName} · ${row.model}", fontWeight = FontWeight.Medium)
                        Text("${row.requestCount} requests · ${formatTokens(row.totalTokens)} tokens")
                        Text(
                            "$${String.format(Locale.US, "%.4f", row.estimatedUsd)}",
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            item {
                Text("Daily", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            }
            items(state.usageByDay) { day ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
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
