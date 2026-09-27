package com.skillmcp.mentor.ui.components.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.R
import com.skillmcp.mentor.ui.chat.SkillSuggestionItem
import com.skillmcp.mentor.ui.chat.SkillSuggestionUi

@Composable
private fun verifiedTint(): Color = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFF6EE7B7) else Color(0xFF047857)

/** "Ready: <skill> · Verified official · <Company>" card shown above the composer. */
@Composable
fun SkillSuggestionCard(
    suggestion: SkillSuggestionUi,
    onUse: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenDocs: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = verifiedTint()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when {
                suggestion.searching ->
                    StatusLine(
                        text = stringResource(R.string.skill_suggest_searching, suggestion.searchingCompany ?: stringResource(R.string.skill_suggest_official)),
                        onDismiss = onDismiss,
                    ) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) }
                suggestion.noOfficialCompany != null -> {
                    StatusLine(
                        text = stringResource(R.string.skill_suggest_none_found, suggestion.noOfficialCompany),
                        onDismiss = onDismiss,
                    ) { Icon(Icons.Rounded.SearchOff, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    Text(
                        stringResource(R.string.skill_suggest_none_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 10.dp),
                    )
                    suggestion.docsUrl?.let { url ->
                        TextButton(onClick = { onOpenDocs(url) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                stringResource(R.string.skill_suggest_docs, suggestion.noOfficialCompany),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }
                suggestion.unavailableCompany != null ->
                    StatusLine(
                        text = stringResource(R.string.skill_suggest_unavailable, suggestion.unavailableCompany),
                        onDismiss = onDismiss,
                    ) { Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(20.dp)) }
                else -> {
                    suggestion.items.forEachIndexed { index, item ->
                        SuggestionRow(item, tint, onUse = { onUse(item.skill.id) }, onDismiss = onDismiss, showDismiss = index == 0)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(
    text: String,
    onDismiss: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.skill_suggest_not_now))
        }
    }
}

@Composable
private fun SuggestionRow(
    item: SkillSuggestionItem,
    tint: Color,
    onUse: () -> Unit,
    onDismiss: () -> Unit,
    showDismiss: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (item.active) Icons.Rounded.CheckCircle else Icons.Rounded.Verified,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Text(
                stringResource(if (item.active) R.string.skill_suggest_active else R.string.skill_suggest_ready, item.skill.name),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            if (showDismiss && !item.active) {
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.skill_suggest_not_now)) }
            }
        }
        Text(
            if (item.foundOnGitHub) {
                stringResource(R.string.skill_suggest_found_badge, item.skill.company)
            } else {
                stringResource(R.string.finder_badge, item.skill.company)
            } + " · " + item.skill.repoFullName,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 28.dp, end = 10.dp),
        )
        if (item.active) {
            Text(
                stringResource(R.string.skill_suggest_active_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 28.dp, end = 10.dp),
            )
        } else {
            Text(
                item.skill.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 28.dp, end = 10.dp),
            )
            FilledTonalButton(onClick = onUse, modifier = Modifier.padding(start = 28.dp).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.skill_suggest_use))
            }
        }
    }
}
