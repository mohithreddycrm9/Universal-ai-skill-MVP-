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
import androidx.compose.foundation.lazy.items
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
    AppBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ScreenHeader(
                    title = "Extensions",
                    subtitle = "Skill packs and on-device plugins to extend your assistant.",
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
                Text("On-device plugins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Enable plugins, then use slash commands in chat (e.g. /calc, /time).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.builtinPlugins, key = { it.id }) { plugin ->
                val enabled = plugin.id in state.prefs.enabledPluginIds
                GlassCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(plugin.title, fontWeight = FontWeight.SemiBold)
                            Text(plugin.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                plugin.commands.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Switch(checked = enabled, onCheckedChange = { vm.setPluginEnabled(plugin.id, it) })
                    }
                }
            }

            item {
                Text("Built-in skill packs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Offline install — no download required.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(state.bundledSkillPacks, key = { it.assetPath }) { pack ->
                GlassCard {
                    Text(pack.title, fontWeight = FontWeight.SemiBold)
                    Text(pack.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(pack.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedButton(onClick = { vm.installBundledSkill(pack) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Install")
                    }
                }
            }

            item {
                Text("Remote catalog", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                    OutlinedButton(
                        onClick = { vm.installCatalogSkill(entry.sourceUrl) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Text("Install")
                    }
                }
            }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Custom source", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
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
}
