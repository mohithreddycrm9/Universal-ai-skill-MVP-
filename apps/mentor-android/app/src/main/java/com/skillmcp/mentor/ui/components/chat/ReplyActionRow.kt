package com.skillmcp.mentor.ui.components.chat

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.R
import kotlinx.coroutines.delay

/** What an assistant reply can do (copy, read aloud, regenerate, share, switch versions). */
data class ReplyActions(
    val isSpeaking: Boolean = false,
    val onToggleSpeak: () -> Unit = {},
    /** Only the latest reply can be regenerated; null hides the button. */
    val onRegenerate: (() -> Unit)? = null,
    val versionIndex: Int = 0,
    val versionCount: Int = 1,
    val onSelectVersion: (Int) -> Unit = {},
)

/** Icon row under an assistant reply, like the ChatGPT / Gemini / Claude apps. 48dp touch targets. */
@Composable
fun ReplyActionRow(
    content: String,
    actions: ReplyActions,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1_800)
            copied = false
        }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        if (actions.versionCount > 1) {
            val label = stringResource(R.string.chat_version_of, actions.versionIndex + 1, actions.versionCount)
            ActionIcon(
                Icons.Rounded.ChevronLeft,
                stringResource(R.string.chat_version_previous),
                enabled = actions.versionIndex > 0,
            ) { actions.onSelectVersion(actions.versionIndex - 1) }
            Text(
                "${actions.versionIndex + 1} / ${actions.versionCount}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = label },
            )
            ActionIcon(
                Icons.Rounded.ChevronRight,
                stringResource(R.string.chat_version_next),
                enabled = actions.versionIndex < actions.versionCount - 1,
            ) { actions.onSelectVersion(actions.versionIndex + 1) }
        }
        ActionIcon(
            if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
            stringResource(if (copied) R.string.chat_copied else R.string.action_copy),
        ) {
            clipboard.setText(AnnotatedString(content))
            copied = true
        }
        ActionIcon(
            if (actions.isSpeaking) Icons.Rounded.StopCircle else Icons.AutoMirrored.Rounded.VolumeUp,
            stringResource(if (actions.isSpeaking) R.string.chat_stop_reading else R.string.chat_read_aloud),
            onClick = actions.onToggleSpeak,
        )
        actions.onRegenerate?.let { regenerate ->
            ActionIcon(Icons.Rounded.Refresh, stringResource(R.string.chat_regenerate), onClick = regenerate)
        }
        ActionIcon(Icons.Rounded.Share, stringResource(R.string.action_share)) {
            val send =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, content)
                }
            context.startActivity(Intent.createChooser(send, null))
        }
    }
}

@Composable
private fun ActionIcon(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            icon,
            contentDescription = description,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
