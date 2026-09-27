package com.skillmcp.mentor.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillmcp.mentor.R
import com.skillmcp.mentor.skills.finder.OfficialSkill
import com.skillmcp.mentor.skills.finder.OfficialSkillPolicy
import com.skillmcp.mentor.skills.finder.OfficialSkillSearch
import com.skillmcp.mentor.skills.finder.SkillCategory
import com.skillmcp.mentor.skills.finder.OfficialSkillFinder
import com.skillmcp.mentor.skills.finder.SkillIndexSource
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.SkillFinderUiState
import com.skillmcp.mentor.ui.SkillFinderViewModel
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.SecondaryScreenTopBar
import com.skillmcp.mentor.ui.components.discover.DiscoverSearchField
import com.skillmcp.mentor.ui.theme.MentorDimens
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SkillFinderScreen(
    vm: MentorViewModel,
    finder: OfficialSkillFinder,
    onBack: () -> Unit,
) {
    val finderVm: SkillFinderViewModel = viewModel(factory = SkillFinderViewModel.Factory(finder))
    val finderState by finderVm.uiState.collectAsState()
    val appState by vm.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<SkillCategory?>(null) }
    var company by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    val installedIds = remember(appState.skills) { appState.skills.map { it.id }.toSet() }
    val selected = finderState.snapshot?.skills?.firstOrNull { it.id == selectedId }
    BackHandler(enabled = selected != null) { selectedId = null }
    SkillFinderContent(
        state = finderState,
        query = query,
        onQueryChange = { query = it },
        category = category,
        onCategoryChange = { category = it },
        company = company,
        onCompanyChange = { company = it },
        selected = selected,
        onSelect = { selectedId = it.id },
        onCloseDetail = { selectedId = null },
        installedIds = installedIds,
        onInstall = { skill ->
            // Re-check at the last moment: only an allowlisted HTTPS GitHub folder can be installed.
            if (OfficialSkillPolicy.verifyUrl(skill.skillUrl).accepted) {
                vm.requestInstallFromUrl(
                    url = skill.skillUrl,
                    title = skill.name,
                    trustTier = "Verified official · ${skill.company}",
                    needsNetwork = true,
                )
            }
        },
        onUseInChat = { skill -> vm.openChatWithSuggestion("Use the ${skill.name} skill to help me with ") },
        onOpenLink = { url -> if (OfficialSkillPolicy.verifyUrl(url).accepted) uriHandler.openUri(url) },
        onRefresh = finderVm::refresh,
        onBack = onBack,
    )
}

@Composable
internal fun SkillFinderContent(
    state: SkillFinderUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    category: SkillCategory?,
    onCategoryChange: (SkillCategory?) -> Unit,
    company: String?,
    onCompanyChange: (String?) -> Unit,
    selected: OfficialSkill?,
    onSelect: (OfficialSkill) -> Unit,
    onCloseDetail: () -> Unit,
    installedIds: Set<String>,
    onInstall: (OfficialSkill) -> Unit,
    onUseInChat: (OfficialSkill) -> Unit,
    onOpenLink: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    if (selected != null) {
        SkillFinderDetail(
            skill = selected,
            installed = selected.installedSkillId in installedIds,
            onBack = onCloseDetail,
            onInstall = { onInstall(selected) },
            onUseInChat = { onUseInChat(selected) },
            onOpenLink = onOpenLink,
        )
        return
    }
    val skills = state.snapshot?.skills.orEmpty()
    val results = remember(skills, query, category, company) { OfficialSkillSearch.search(skills, query, category, company) }
    val categories = remember(skills) { OfficialSkillSearch.categories(skills) }
    val companies = remember(skills) { OfficialSkillSearch.companies(skills) }
    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                SecondaryScreenTopBar(
                    title = stringResource(R.string.finder_title),
                    subtitle = stringResource(R.string.finder_subtitle),
                    onBack = onBack,
                )
            },
        ) { inner ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(horizontal = MentorDimens.ScreenHorizontal, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item("search") {
                    DiscoverSearchField(
                        query = query,
                        onQueryChange = onQueryChange,
                        placeholder = stringResource(R.string.finder_search_placeholder),
                    )
                }
                item("categories") {
                    ChipRow {
                        FinderChip(stringResource(R.string.discover_all), category == null) { onCategoryChange(null) }
                        categories.forEach { c ->
                            FinderChip(stringResource(c.labelRes()), category == c) { onCategoryChange(if (category == c) null else c) }
                        }
                    }
                }
                item("companies") {
                    ChipRow {
                        FinderChip(stringResource(R.string.finder_all_companies), company == null) { onCompanyChange(null) }
                        companies.forEach { name ->
                            FinderChip(name, company == name) { onCompanyChange(if (company == name) null else name) }
                        }
                    }
                }
                item("status") { FinderStatusRow(state, onRefresh) }
                if (results.isEmpty()) {
                    item("empty") {
                        FinderEmptyState(
                            filtered = query.isNotBlank() || category != null || company != null,
                            onClear = {
                                onQueryChange("")
                                onCategoryChange(null)
                                onCompanyChange(null)
                            },
                        )
                    }
                } else {
                    item("count") {
                        Text(
                            pluralStringResource(R.plurals.finder_result_count, results.size, results.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(results, key = { it.id }) { skill ->
                        FinderSkillCard(skill, installed = skill.installedSkillId in installedIds, onClick = { onSelect(skill) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun FinderChip(
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
private fun FinderStatusRow(
    state: SkillFinderUiState,
    onRefresh: () -> Unit,
) {
    val snapshot = state.snapshot
    val text =
        when {
            state.refreshing -> stringResource(R.string.finder_status_checking)
            snapshot == null -> stringResource(R.string.finder_status_checking)
            snapshot.rateLimited -> stringResource(R.string.finder_status_rate_limited)
            snapshot.failed -> stringResource(R.string.finder_status_offline)
            snapshot.source == SkillIndexSource.BUNDLED ->
                stringResource(R.string.finder_status_bundled, formatSkillDate(snapshot.indexGeneratedAt))
            else -> stringResource(R.string.finder_status_checked, formatSkillDate(Instant.ofEpochMilli(snapshot.lastRefreshAt).toString()))
        }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (snapshot != null && (snapshot.failed || snapshot.rateLimited)) {
            Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(start = 6.dp),
        )
        IconButton(onClick = onRefresh, enabled = !state.refreshing) {
            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.finder_refresh))
        }
    }
}

@Composable
private fun verifiedAccent(): Color = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFF6EE7B7) else Color(0xFF047857)

@Composable
internal fun VerifiedOfficialBadge(company: String) {
    val accent = verifiedAccent()
    Surface(
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Verified, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text(
                stringResource(R.string.finder_badge, company),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun FinderSkillCard(
    skill: OfficialSkill,
    installed: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    skill.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (installed) {
                    Text(stringResource(R.string.finder_installed), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            VerifiedOfficialBadge(skill.company)
            Text(
                skill.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "github.com/${skill.repoFullName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FinderEmptyState(
    filtered: Boolean,
    onClear: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.SearchOff, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(R.string.finder_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.finder_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (filtered) {
                TextButton(onClick = onClear, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.finder_clear_filters)) }
            }
        }
    }
}

@Composable
internal fun SkillFinderDetail(
    skill: OfficialSkill,
    installed: Boolean,
    onBack: () -> Unit,
    onInstall: () -> Unit,
    onUseInChat: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { SecondaryScreenTopBar(title = skill.name, subtitle = null, onBack = onBack) },
        ) { inner ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MentorDimens.ScreenHorizontal, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    skill.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() },
                )
                VerifiedOfficialBadge(skill.company)
                Text(skill.description, style = MaterialTheme.typography.bodyLarge)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailRow(stringResource(R.string.finder_detail_publisher), "${skill.company} (github.com/${skill.orgLogin})")
                        DetailRow(stringResource(R.string.finder_detail_repo), skill.repoFullName, link = skill.repoUrl, onOpenLink = onOpenLink)
                        DetailRow(stringResource(R.string.finder_detail_folder), skill.skillUrl.removePrefix("https://"), link = skill.skillUrl, onOpenLink = onOpenLink)
                        DetailRow(
                            stringResource(R.string.finder_detail_license),
                            skill.license.ifBlank { stringResource(R.string.finder_license_not_declared) },
                        )
                        DetailRow(stringResource(R.string.finder_detail_updated), formatSkillDate(skill.lastUpdated))
                        DetailRow(stringResource(R.string.finder_detail_category), stringResource(skill.category.labelRes()))
                    }
                }
                if (installed) {
                    Button(onClick = onUseInChat, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.finder_use_in_chat), modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(
                        stringResource(R.string.finder_installed_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Button(onClick = onInstall, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.finder_install), modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(
                        stringResource(R.string.finder_install_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = { onOpenLink(skill.skillUrl) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.finder_view_source), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    link: String? = null,
    onOpenLink: (String) -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (link != null) Modifier.clickable { onOpenLink(link) } else Modifier),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (link != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (link != null) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

internal fun SkillCategory.labelRes(): Int =
    when (this) {
        SkillCategory.DOCUMENTS -> R.string.finder_cat_documents
        SkillCategory.SECURITY -> R.string.finder_cat_security
        SkillCategory.DESIGN -> R.string.finder_cat_design
        SkillCategory.WRITING -> R.string.finder_cat_writing
        SkillCategory.DATA_AI -> R.string.finder_cat_data_ai
        SkillCategory.CLOUD -> R.string.finder_cat_cloud
        SkillCategory.CODING -> R.string.finder_cat_coding
        SkillCategory.GENERAL -> R.string.finder_cat_general
    }

/** ISO-8601 timestamp to a localized medium date in the device zone; unparsable input is shown as-is. */
internal fun formatSkillDate(iso: String, zone: ZoneId = ZoneId.systemDefault()): String =
    runCatching {
        OffsetDateTime.parse(iso).atZoneSameInstant(zone).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }.getOrDefault(iso)
