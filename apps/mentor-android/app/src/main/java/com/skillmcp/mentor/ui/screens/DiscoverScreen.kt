package com.skillmcp.mentor.ui.screens

import com.skillmcp.mentor.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.llm.isConfigured
import com.skillmcp.mentor.mentor.PopularUseCase
import com.skillmcp.mentor.mentor.UseCaseCatalog
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.discover.DiscoverCategorySections
import com.skillmcp.mentor.ui.components.discover.DiscoverHeroCard
import com.skillmcp.mentor.ui.components.discover.DiscoverSearchField
import com.skillmcp.mentor.ui.components.discover.WorkflowVisualCard
import com.skillmcp.mentor.ui.components.discover.displayName
import com.skillmcp.mentor.ui.components.settings.SettingsDivider
import com.skillmcp.mentor.ui.components.settings.SettingsGroup
import com.skillmcp.mentor.ui.components.settings.SettingsNavRow
import com.skillmcp.mentor.ui.theme.CategoryPalette
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
        onOpenSkillFinder = { vm.requestOpenTab("skill-finder") },
    )
}

@Composable
internal fun DiscoverScreenContent(
    state: com.skillmcp.mentor.ui.MentorUiState,
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    filteredUseCases: List<PopularUseCase> = state.rankedUseCases,
    onConnect: (String) -> Unit,
    onPopularUseCase: (PopularUseCase) -> Unit,
    onOpenUsage: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenSkillFinder: () -> Unit = {},
) {
    val profile = state.activeLlmProfile
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val filtering = searchQuery.isNotBlank() || category != null
    val visible = filteredUseCases.filter { category == null || it.category == category }
    val workflowOfDay = state.rankedUseCases.firstOrNull()

    AppBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = MentorDimens.ScreenHorizontal, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("search") { DiscoverSearchField(query = searchQuery, onQueryChange = onSearchChange) }
            item("chips") {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CategoryChip(stringResource(R.string.discover_all), selected = category == null) { category = null }
                    UseCaseCatalog.categories.forEach { c ->
                        val style = CategoryPalette.styleFor(c)
                        CategoryChip(style.displayName(c), selected = category == c) {
                            category = if (category == c) null else c
                        }
                    }
                }
            }
            if (profile != null && !profile.isConfigured()) {
                item("connect") { ConnectModelCard(profile.name) { onConnect(profile.id) } }
            }
            if (!filtering && workflowOfDay != null) {
                item("hero") { DiscoverHeroCard(workflow = workflowOfDay, onTry = { onPopularUseCase(workflowOfDay) }) }
            }
            when {
                visible.isEmpty() ->
                    item("empty") {
                        Text(
                            stringResource(R.string.discover_no_results),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        )
                    }
                filtering ->
                    items(visible, key = { it.id }) { useCase ->
                        WorkflowVisualCard(useCase, onClick = { onPopularUseCase(useCase) }, modifier = Modifier.fillMaxWidth())
                    }
                else ->
                    item("sections") {
                        DiscoverCategorySections(
                            categories = UseCaseCatalog.categories,
                            useCases = visible,
                            onUseCase = onPopularUseCase,
                        )
                    }
            }
            if (!filtering) {
                item("shortcuts") {
                    SettingsGroup(title = stringResource(R.string.discover_shortcuts)) {
                        SettingsNavRow(
                            icon = Icons.Rounded.Verified,
                            title = stringResource(R.string.finder_title),
                            subtitle = stringResource(R.string.finder_entry_subtitle),
                            onClick = onOpenSkillFinder,
                        )
                        SettingsDivider()
                        SettingsNavRow(
                            icon = Icons.Rounded.BarChart,
                            title = stringResource(R.string.discover_open_activity),
                            onClick = onOpenUsage,
                        )
                        SettingsDivider()
                        SettingsNavRow(
                            icon = Icons.Rounded.Key,
                            title = stringResource(R.string.settings_models_keys),
                            onClick = onOpenModels,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        shape = RoundedCornerShape(50),
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

@Composable
private fun ConnectModelCard(
    profileName: String,
    onConnect: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    stringResource(R.string.discover_connect_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = 10.dp).semantics { heading() },
                )
            }
            Text(
                stringResource(R.string.discover_connect_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(onClick = onConnect, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.discover_connect_action, profileName))
            }
        }
    }
}
