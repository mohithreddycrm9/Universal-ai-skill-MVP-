package com.skillmcp.mentor.ui.components.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.R
import com.skillmcp.mentor.llm.ConversationContext
import com.skillmcp.mentor.llm.LlmProfile
import com.skillmcp.mentor.llm.LlmProviderKind
import com.skillmcp.mentor.llm.VisionCapabilities
import com.skillmcp.mentor.llm.isConfigured

/** Top-bar chip showing the chat's model; tapping opens the switcher. */
@Composable
fun ModelChip(
    profile: LlmProfile?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = profile?.let { ConversationContext.modelLabel(it) } ?: stringResource(R.string.model_sheet_title)
    val cd = stringResource(R.string.model_chip_cd, label)
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 32.dp)
            .semantics(mergeDescendants = true) { contentDescription = cd }
            .padding(start = 2.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (profile?.isConfigured() == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp),
        )
        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

/** Thin centered divider row: "Switched to llama3.2". */
@Composable
fun ModelSwitchDivider(
    modelLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Text(
                    stringResource(R.string.model_switched_to, modelLabel),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSwitcherSheet(
    profiles: List<LlmProfile>,
    currentId: String?,
    onPick: (LlmProfile) -> Unit,
    onConnect: (LlmProfile) -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        ModelSwitcherContent(profiles, currentId, onPick, onConnect, onManage)
    }
}

/** Sheet body (separate so snapshot tests can render it without a window/popup). */
@Composable
fun ModelSwitcherContent(
    profiles: List<LlmProfile>,
    currentId: String?,
    onPick: (LlmProfile) -> Unit,
    onConnect: (LlmProfile) -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (connected, notConnected) = profiles.partition { it.isConfigured() }
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.model_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.model_sheet_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (connected.isNotEmpty()) {
            SectionLabel(stringResource(R.string.model_sheet_connected))
            connected.forEach { p -> ModelRow(p, selected = p.id == currentId, connected = true) { onPick(p) } }
        }
        if (notConnected.isNotEmpty()) {
            SectionLabel(stringResource(R.string.model_sheet_not_connected))
            notConnected.forEach { p -> ModelRow(p, selected = false, connected = false) { onConnect(p) } }
        }
        TextButton(onClick = onManage, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.model_sheet_manage), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp).semantics { heading() },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelRow(
    profile: LlmProfile,
    selected: Boolean,
    connected: Boolean,
    onClick: () -> Unit,
) {
    val local = profile.kind == LlmProviderKind.OLLAMA
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).semantics { this.selected = selected },
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (local) Icons.Rounded.Home else Icons.Rounded.Cloud,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    ConversationContext.modelLabel(profile),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    profile.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (VisionCapabilities.supportsVision(profile)) Capability(stringResource(R.string.model_cap_images))
                    if (local) Capability(stringResource(R.string.model_cap_local))
                    Capability(stringResource(R.string.model_cap_context, formatTokens(ConversationContext.contextWindowTokens(profile))))
                }
            }
            when {
                selected ->
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = stringResource(R.string.model_sheet_current),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                !connected ->
                    Text(
                        stringResource(R.string.model_sheet_connect),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
            }
        }
    }
}

@Composable
private fun Capability(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** 4096 -> "4K", 128000 -> "128K", 1000000 -> "1M". */
fun formatTokens(tokens: Int): String =
    when {
        tokens >= 1_000_000 -> "${tokens / 1_000_000}M"
        tokens >= 1_000 && tokens % 1_000 == 0 -> "${tokens / 1_000}K"
        tokens >= 1_024 -> "${tokens / 1_024}K"
        else -> tokens.toString()
    }
