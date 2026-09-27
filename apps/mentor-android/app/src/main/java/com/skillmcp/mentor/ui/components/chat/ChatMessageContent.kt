package com.skillmcp.mentor.ui.components.chat

import com.skillmcp.mentor.R
import androidx.compose.ui.res.stringResource
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.skillmcp.mentor.ui.theme.BrandColors
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.ui.util.performLongPressHaptic
import com.skillmcp.mentor.ui.util.rememberHapticView
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageContent(
    content: String,
    isUser: Boolean,
    isStreaming: Boolean,
    modelLabel: String?,
    modifier: Modifier = Modifier,
    onReply: ((String) -> Unit)? = null,
) {
    val view = rememberHapticView()
    var menuOpen by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val longPress =
        Modifier.combinedClickable(
            onClick = {},
            onLongClick = {
                view.performLongPressHaptic()
                menuOpen = true
            },
        )
    if (isUser) {
        // User turns: tinted bubble on the right, max ~85% width.
        BoxWithConstraints(
            modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = maxWidth * 0.85f).then(longPress),
                shape = UserBubbleShape,
                color = userBubbleColor(),
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    } else {
        // Assistant turns: full width on the left, no container.
        Column(
            modifier = modifier.fillMaxWidth().then(longPress).padding(vertical = 10.dp),
        ) {
            AssistantMarkdownText(content = content, showCursor = isStreaming)
            if (!isStreaming && modelLabel != null) {
                Text(
                    modelLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_copy)) },
            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
            onClick = {
                clipboard.setText(AnnotatedString(content))
                menuOpen = false
            },
        )
        if (onReply != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_reply)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Reply, null) },
                onClick = {
                    onReply(content)
                    menuOpen = false
                },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_share)) },
            leadingIcon = { Icon(Icons.Outlined.Share, null) },
            onClick = {
                val send =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, content)
                    }
                context.startActivity(Intent.createChooser(send, null))
                menuOpen = false
            },
        )
    }
}

@Composable
private fun AssistantMarkdownText(
    content: String,
    showCursor: Boolean,
) {
    val clipboard = LocalClipboardManager.current
    var parts by remember(content) { mutableStateOf(content.split("```")) }
    LaunchedEffect(content) {
        parts = MarkdownPartsCache.splitFencedBlocks(content)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 0) {
                if (part.isNotBlank()) {
                    MarkdownText(
                        markdown = part.trim(),
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    )
                }
            } else {
                val lines = part.trim().lines()
                val code = if (lines.size > 1) lines.drop(1).joinToString("\n") else part.trim()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.Top,
                    ) {
                        Text(
                            text = code,
                            modifier = Modifier.weight(1f).padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(
                            onClick = { clipboard.setText(AnnotatedString(code)) },
                            modifier = Modifier.padding(4.dp),
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.chat_copy_code))
                        }
                    }
                }
            }
        }
        if (showCursor) {
            Text(
                "▍",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private val UserBubbleShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 6.dp, bottomStart = 20.dp)

/** Indigo at 11% on light surfaces; #1E2130 on dark surfaces. */
@Composable
internal fun userBubbleColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        UserBubbleDark
    } else {
        BrandColors.Indigo.copy(alpha = USER_BUBBLE_LIGHT_ALPHA)
    }

internal const val USER_BUBBLE_LIGHT_ALPHA = 0.11f
internal val UserBubbleDark = Color(0xFF1E2130)
