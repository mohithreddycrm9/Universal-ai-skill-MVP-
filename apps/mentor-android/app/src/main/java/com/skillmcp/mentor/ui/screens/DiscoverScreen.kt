package com.skillmcp.mentor.ui.screens

import com.skillmcp.mentor.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.llm.isConfigured
import com.skillmcp.mentor.mentor.UseCaseCatalog
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.GlassCard
import com.skillmcp.mentor.ui.components.StatCard
import com.skillmcp.mentor.ui.components.discover.DiscoverCategorySections
import com.skillmcp.mentor.ui.components.discover.DiscoverHeroCard
import com.skillmcp.mentor.ui.components.discover.DiscoverSearchField
import com.skillmcp.mentor.ui.theme.MentorDimens

@Composable
fun DiscoverScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered =
        remember(state.rankedUseCases, query) {
            if (query.isBlank()) {
                state.rankedUseCases
            } else {
                state.rankedUseCases.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.subtitle.contains(query, ignoreCase = true)
                }
            }
        }
    DiscoverScreenContent(
        state = state,
        searchQuery = query,
        onSearchChange = { query = it },
        filteredUseCases = filtered,
        onConnect = vm::openConnectLlm,
        onPopularUseCase = vm::startPopularUseCase,
        onOpenUsage = { vm.requestOpenTab("usage") },
        onOpenModels = { vm.requestOpenTab("models") },
    )
}

@Composable
internal fun DiscoverScreenContent(
    state: com.skillmcp.mentor.ui.MentorUiState,
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    filteredUseCases: List<com.skillmcp.mentor.mentor.PopularUseCase> = state.rankedUseCases,
    onConnect: (String) -> Unit,
    onPopularUseCase: (com.skillmcp.mentor.mentor.PopularUseCase) -> Unit,
    onOpenUsage: () -> Unit,
    onOpenModels: () -> Unit,
) {
    val profile = state.activeLlmProfile
    val workflowOfDay = state.rankedUseCases.firstOrNull()

    AppBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = MentorDimens.ScreenHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DiscoverSearchField(query = searchQuery, onQueryChange = onSearchChange)
            }
            item {
                DiscoverHeroCard(
                    workflow = workflowOfDay,
                    onTry = { workflowOfDay?.let { onPopularUseCase(it) } },
                )
            }
            if (profile != null && !profile.isConfigured()) {
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Connect a model to start",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Pick a provider and add an API key or sign in in your browser.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(onClick = { onConnect(profile.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Connect ${profile.name}")
                            }
                        }
                    }
                }
            }
            item {
                DiscoverCategorySections(
                    categories = UseCaseCatalog.categories,
                    useCases = filteredUseCases,
                    onUseCase = onPopularUseCase,
                    iconForCategory = ::discoverCategoryIcon,
                )
            }
            item {
                StatCard(
                    title = stringResource(R.string.discover_messages_week),
                    value = state.usageTotals.requestCount.toString(),
                    subtitle = "${state.conversations.size} active chats",
                )
            }
            item {
                OutlinedButton(onClick = onOpenUsage, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.discover_open_activity))
                }
            }
            item {
                OutlinedButton(onClick = onOpenModels, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_models_keys))
                }
            }
        }
    }
}

private fun discoverCategoryIcon(category: String): ImageVector =
    when (category) {
        "Write" -> Icons.Outlined.Edit
        "Learn" -> Icons.Outlined.School
        "Life" -> Icons.Outlined.Home
        "Shop" -> Icons.Outlined.ShoppingBag
        "Work" -> Icons.Outlined.Work
        "Privacy" -> Icons.Outlined.Lock
        else -> Icons.Outlined.Home
    }
