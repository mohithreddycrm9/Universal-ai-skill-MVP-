package com.skillmcp.mentor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.llm.isConfigured
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.GlassCard
import com.skillmcp.mentor.ui.components.PopularUseCaseCard
import com.skillmcp.mentor.ui.components.ScreenHeader
import com.skillmcp.mentor.ui.components.StatCard

@Composable
fun DiscoverScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    val profile = state.activeLlmProfile

    AppBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ScreenHeader(
                    title = "Discover",
                    subtitle = "Workflows, spend snapshot, and shortcuts to models and abilities.",
                )
            }
            if (profile != null && !profile.isConfigured()) {
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Connect a model to start", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Pick a provider and add an API key or sign in in your browser.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(onClick = { vm.openConnectLlm(profile.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Connect ${profile.name}")
                            }
                        }
                    }
                }
            }
            item {
                Text("Popular workflows", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.rankedUseCases.take(10), key = { it.id }) { useCase ->
                        PopularUseCaseCard(useCase = useCase, onClick = { vm.startPopularUseCase(useCase) })
                    }
                }
            }
            item {
                Text("Estimated spend (7 days)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Amounts are estimates from provider usage logs — not a bill.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                StatCard(
                    title = "Last 7 days",
                    value = "$${"%.2f".format(state.usageTotals.estimatedUsd)}",
                    subtitle =
                        "${state.usageTotals.requestCount} requests · " +
                            "${state.usageTotals.promptTokens + state.usageTotals.completionTokens} tokens",
                )
            }
            state.spendGuard.warningMessage?.let { warning ->
                item {
                    GlassCard {
                        Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                OutlinedButton(onClick = { vm.requestOpenTab("usage") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open full usage dashboard")
                }
            }
            item {
                Text("Privacy-friendly options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("On-device & local models", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Use Ollama on your home network or explore Gemini Nano when your device supports it — no cloud key required for local hosts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(onClick = { vm.requestOpenTab("models") }, modifier = Modifier.fillMaxWidth()) {
                            Text("Set up Ollama or Hugging Face")
                        }
                    }
                }
            }
        }
    }
}
