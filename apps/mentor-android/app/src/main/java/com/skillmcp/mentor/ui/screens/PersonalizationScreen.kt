package com.skillmcp.mentor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.R
import com.skillmcp.mentor.data.MentorPrefs
import com.skillmcp.mentor.llm.ModelPreset
import com.skillmcp.mentor.mentor.PersonalizationPrompt
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.SecondaryScreenTopBar
import com.skillmcp.mentor.ui.components.settings.SettingsBlock
import com.skillmcp.mentor.ui.components.settings.SettingsDivider
import com.skillmcp.mentor.ui.components.settings.SettingsGroup
import com.skillmcp.mentor.ui.components.settings.SettingsSwitchRow
import com.skillmcp.mentor.ui.settings.SettingsScreenActions
import com.skillmcp.mentor.ui.theme.MentorDimens

@Composable
fun PersonalizationScreen(vm: MentorViewModel, onBack: () -> Unit) {
    val state by vm.uiState.collectAsState()
    PersonalizationScreenContent(prefs = state.prefs, vm = vm, onBack = onBack)
}

/** Custom instructions: name, what Lumina should know, how it should respond, response style. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PersonalizationScreenContent(
    prefs: MentorPrefs,
    vm: SettingsScreenActions,
    onBack: () -> Unit,
) {
    val max = PersonalizationPrompt.MAX_FIELD_CHARS
    val traits =
        listOf(
            stringResource(R.string.trait_concise),
            stringResource(R.string.trait_bullets),
            stringResource(R.string.trait_steps),
            stringResource(R.string.trait_casual),
            stringResource(R.string.trait_formal),
            stringResource(R.string.trait_clarify),
        )
    AppBackground {
        Column(Modifier.fillMaxSize()) {
            SecondaryScreenTopBar(
                title = stringResource(R.string.personalization_title),
                subtitle = stringResource(R.string.personalization_subtitle),
                onBack = onBack,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MentorDimens.ScreenHorizontal, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SettingsGroup(title = stringResource(R.string.personalization_about_title)) {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = stringResource(R.string.personalization_use),
                        subtitle = stringResource(R.string.personalization_use_hint),
                        checked = prefs.personalizationEnabled,
                        onCheckedChange = { on -> vm.updatePrefs { it.copy(personalizationEnabled = on) } },
                    )
                    SettingsDivider()
                    SettingsBlock {
                        OutlinedTextField(
                            value = prefs.displayName,
                            onValueChange = { v -> vm.updatePrefs { it.copy(displayName = v.take(80)) } },
                            label = { Text(stringResource(R.string.settings_your_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = prefs.aboutMe,
                            onValueChange = { v -> vm.updatePrefs { it.copy(aboutMe = v.take(max)) } },
                            label = { Text(stringResource(R.string.personalization_about_label)) },
                            placeholder = { Text(stringResource(R.string.personalization_about_hint)) },
                            supportingText = { Text(stringResource(R.string.personalization_char_count, prefs.aboutMe.length, max)) },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                SettingsGroup(title = stringResource(R.string.personalization_respond_title)) {
                    SettingsBlock {
                        OutlinedTextField(
                            value = prefs.responseInstructions,
                            onValueChange = { v -> vm.updatePrefs { it.copy(responseInstructions = v.take(max)) } },
                            label = { Text(stringResource(R.string.personalization_respond_label)) },
                            placeholder = { Text(stringResource(R.string.personalization_respond_hint)) },
                            supportingText = {
                                Text(stringResource(R.string.personalization_char_count, prefs.responseInstructions.length, max))
                            },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            stringResource(R.string.personalization_traits),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            traits.forEach { trait ->
                                SuggestionChip(
                                    onClick = {
                                        vm.updatePrefs {
                                            it.copy(responseInstructions = PersonalizationPrompt.appendTrait(it.responseInstructions, trait))
                                        }
                                    },
                                    label = { Text(trait) },
                                    icon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.padding(0.dp)) },
                                )
                            }
                        }
                    }
                    SettingsDivider()
                    SettingsBlock {
                        Text(stringResource(R.string.settings_response_style), style = MaterialTheme.typography.labelLarge)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            ModelPreset.entries.forEachIndexed { index, preset ->
                                SegmentedButton(
                                    selected = prefs.modelPreset == preset,
                                    onClick = { vm.setModelPreset(preset) },
                                    shape = SegmentedButtonDefaults.itemShape(index, ModelPreset.entries.size),
                                    label = { Text(preset.localizedLabel(), maxLines = 1) },
                                )
                            }
                        }
                        Text(
                            prefs.modelPreset.localizedHint(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                SettingsGroup(title = stringResource(R.string.personalization_advanced)) {
                    SettingsBlock {
                        OutlinedTextField(
                            value = prefs.focusTopic,
                            onValueChange = vm::updateFocusTopic,
                            label = { Text(stringResource(R.string.settings_focus_topic)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = prefs.assistantSystemPrompt,
                            onValueChange = { v -> vm.updatePrefs { it.copy(assistantSystemPrompt = v) } },
                            label = { Text(stringResource(R.string.settings_system_instructions)) },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (prefs.assistantSystemPrompt != MentorPrefs.DEFAULT_ASSISTANT_PROMPT) {
                            TextButton(
                                onClick = { vm.updatePrefs { it.copy(assistantSystemPrompt = MentorPrefs.DEFAULT_ASSISTANT_PROMPT) } },
                            ) { Text(stringResource(R.string.action_reset_default)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModelPreset.localizedLabel(): String =
    when (this) {
        ModelPreset.FAST -> stringResource(R.string.preset_fast)
        ModelPreset.BALANCED -> stringResource(R.string.preset_balanced)
        ModelPreset.DEEP -> stringResource(R.string.preset_deep)
    }

@Composable
fun ModelPreset.localizedHint(): String =
    when (this) {
        ModelPreset.FAST -> stringResource(R.string.preset_fast_hint)
        ModelPreset.BALANCED -> stringResource(R.string.preset_balanced_hint)
        ModelPreset.DEEP -> stringResource(R.string.preset_deep_hint)
    }
