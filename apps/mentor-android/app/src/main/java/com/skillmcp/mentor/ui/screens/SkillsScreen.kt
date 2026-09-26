package com.skillmcp.mentor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.skillmcp.mentor.skills.SkillCatalog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.plugins.BuiltinPlugins
import com.skillmcp.mentor.plugins.PluginKind
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.GlassCard
import com.skillmcp.mentor.mentor.ScreenSuggestions
import com.skillmcp.mentor.mentor.SuggestionScreen
import com.skillmcp.mentor.ui.components.ScreenHeader
import com.skillmcp.mentor.ui.components.PopularUseCaseCard
import com.skillmcp.mentor.ui.components.TabSuggestions

@Composable
fun SkillsScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    var sourceUrl by remember { mutableStateOf("") }
    var confirmFetch by remember { mutableStateOf(false) }
    AppBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ScreenHeader(
                    title = "Add abilities",
                    subtitle = "Teach the assistant new workflows. Calculator and date/time are always built into chat.",
                )
            }
            item {
                TabSuggestions(
                    title = "Ask about extensions",
                    suggestions = ScreenSuggestions.forScreen(SuggestionScreen.EXTENSIONS),
                    onSelect = vm::openChatWithSuggestion,
                )
            }
            item {
                Text("Trending workflows", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Tap to install a matching skill (when available) and open Chat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.popularUseCases.take(10), key = { it.id }) { useCase ->
                        PopularUseCaseCard(useCase = useCase, onClick = { vm.startPopularUseCase(useCase) })
                    }
                }
            }

            item {
                Text("Built-in tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Always available in chat — no install or toggle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                GlassCard {
                    BuiltinPlugins.builtIn.forEach { tool ->
                        Text(tool.title, fontWeight = FontWeight.Medium)
                        Text(
                            tool.commands.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
            }
            item {
                Text("Network tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Optional tools that can fetch HTTPS pages when you enable them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.builtinPlugins.filter { it.kind == PluginKind.OPTIONAL }, key = { it.id }) { plugin ->
                val enabled = plugin.id in state.prefs.enabledPluginIds
                GlassCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(plugin.title, fontWeight = FontWeight.SemiBold)
                            Text(plugin.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                plugin.commands.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { on ->
                                if (on && plugin.id == "fetch") {
                                    confirmFetch = true
                                } else {
                                    vm.setPluginEnabled(plugin.id, on)
                                }
                            },
                        )
                    }
                }
            }

            item {
                Text("Offline abilities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Shipped with the app — no download.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(state.bundledSkillPacks, key = { it.assetPath }) { pack ->
                val installed = SkillCatalog.isBundledInstalled(pack, state.skills)
                GlassCard {
                    Text(pack.title, fontWeight = FontWeight.SemiBold)
                    Text(pack.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(pack.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (installed) {
                        Text("Installed", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    } else {
                        OutlinedButton(onClick = { vm.installBundledSkill(pack) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text("Install")
                        }
                    }
                }
            }

            item {
                Text("Add from catalog", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Curated instruction packs from public repositories.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.catalogSkills, key = { it.id }) { entry ->
                GlassCard {
                    Text(entry.title, fontWeight = FontWeight.SemiBold)
                    Text(entry.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${entry.category} · ${entry.trustTier}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Source: ${entry.sourceUrl}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { vm.installCatalogSkill(entry) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Text("Review & install")
                    }
                }
            }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Install from link", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Paste a public skill repository URL.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = sourceUrl,
                            onValueChange = { sourceUrl = it },
                            label = { Text("Skill source URL") },
                            placeholder = { Text("https://…/owner/repo") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
                            shape = MaterialTheme.shapes.medium,
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                                ),
                        )
                        Button(
                            onClick = { vm.importSkill(sourceUrl) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Text("Install skill")
                        }
                    }
                }
            }
            item {
                Text("Installed skills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (state.skills.isEmpty()) {
                item {
                    GlassCard {
                        Text("No skills installed yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(state.skills, key = { it.id }) { skill ->
                val enabled = state.skillToggles[skill.id] != false
                GlassCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(skill.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (skill.owner == "bundled") "Built-in pack" else "${skill.owner} · ${skill.repo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = enabled, onCheckedChange = { vm.setSkillEnabled(skill.id, it) })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { vm.removeSkill(skill.id) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove skill")
                        }
                    }
                }
            }
        }
    }
    if (confirmFetch) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmFetch = false },
            title = { Text("Allow network access?") },
            text = {
                Text(
                    "The /fetch tool can download HTTPS pages you request in chat. Only enable if you trust the sites you share.",
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        vm.setPluginEnabled("fetch", true)
                        confirmFetch = false
                    },
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmFetch = false }) { Text("Cancel") }
            },
        )
    }
}
