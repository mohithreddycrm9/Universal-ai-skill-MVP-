package com.skillmcp.mentor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.skillmcp.mentor.ui.components.ScreenHeader

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
                    title = "Skills",
                    subtitle = "Extend your assistant with curated packs. Toggle skills per conversation in Chat.",
                )
            }
            item {
                Text("Featured catalog", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                Text("Installed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (state.skills.isEmpty()) {
                item {
                    GlassCard {
                        Text("No skills installed yet. Pick one from the catalog above.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                "${skill.owner} · ${skill.repo}",
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
