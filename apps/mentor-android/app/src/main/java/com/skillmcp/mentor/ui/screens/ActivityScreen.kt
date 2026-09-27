package com.skillmcp.mentor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.skillmcp.mentor.ui.components.SecondaryScreenTopBar
import com.skillmcp.mentor.ui.components.StatCard
import com.skillmcp.mentor.mentor.PopularUseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(vm: MentorViewModel, onBack: (() -> Unit)? = null) {
    val state by vm.uiState.collectAsState()
    val totals = state.usageTotals

    AppBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                if (onBack != null) {
                    SecondaryScreenTopBar(
                        title = "Activity",
                        subtitle = "Messages, chats, and workflows you have used.",
                        onBack = onBack,
                    )
                }
            },
        ) { innerPadding ->
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
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
                        StatCard(
                            title = "Messages sent",
                            value = totals.requestCount.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            title = "Chats",
                            value = state.conversations.size.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Text("Top workflows", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(state.popularUseCases.take(5)) { useCase: PopularUseCase ->
                    GlassCard {
                        Text(useCase.title, fontWeight = FontWeight.Medium)
                        Text(
                            useCase.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
                item {
                    Text("Message limits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Optional caps to pace heavy use. Limits never show currency.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        value = if (state.prefs.dailyMessageLimit > 0) state.prefs.dailyMessageLimit.toString() else "",
                        onValueChange = { v ->
                            val n = v.filter { it.isDigit() }.toIntOrNull() ?: 0
                            vm.updatePrefs { p -> p.copy(dailyMessageLimit = n) }
                        },
                        label = { Text("Daily message cap (0 = off)") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        value = if (state.prefs.weeklyMessageLimit > 0) state.prefs.weeklyMessageLimit.toString() else "",
                        onValueChange = { v ->
                            val n = v.filter { it.isDigit() }.toIntOrNull() ?: 0
                            vm.updatePrefs { p -> p.copy(weeklyMessageLimit = n) }
                        },
                        label = { Text("Weekly message cap (0 = off)") },
                        singleLine = true,
                    )
                }
                if (state.usageByModel.isNotEmpty()) {
                    item {
                        Text("By model", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    items(state.usageByModel) { row ->
                        GlassCard {
                            Text("${row.providerName} · ${row.model}", fontWeight = FontWeight.Medium)
                            Text(
                                "${row.requestCount} messages",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
