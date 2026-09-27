package com.skillmcp.mentor.ui.components.chat

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.R
import com.skillmcp.mentor.ui.theme.BrandColors
import com.skillmcp.mentor.ui.util.performLongPressHaptic
import com.skillmcp.mentor.ui.util.rememberHapticView
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.delay

/**
 * One chat turn. User turns: tinted bubble on the right (long-press: copy / edit / reply / share).
 * Assistant turns: full width with a Lumina mark and model name above, and an action row below.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageContent(
    content: String,
    isUser: Boolean,
    isStreaming: Boolean,
    modelLabel: String?,
    modifier: Modifier = Modifier,
    onReply: ((String) -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    actions: ReplyActions? = null,
    isBeingEdited: Boolean = false,
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
        BoxWithConstraints(
            modifier = modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            val bubbleMax = maxWidth * 0.84f
            Box {
                Surface(
                    modifier = Modifier.widthIn(max = bubbleMax).clip(UserBubbleShape).then(longPress),
                    shape = UserBubbleShape,
                    color = if (isBeingEdited) MaterialTheme.colorScheme.primaryContainer else userBubbleColor(),
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    )
                }
                UserMessageMenu(
                    expanded = menuOpen,
                    onDismiss = { menuOpen = false },
                    onCopy = { clipboard.setText(AnnotatedString(content)) },
                    onEdit = onEdit,
                    onReply = onReply?.let { { it(content) } },
                    onShare = { shareText(context, content) },
                )
            }
        }
    } else {
        Column(modifier = modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp)) {
            AssistantHeader(modelLabel)
            Box(Modifier.fillMaxWidth().then(longPress).padding(top = 8.dp)) {
                AssistantMarkdownText(content = content, showCursor = isStreaming)
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_copy)) },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                        onClick = {
                            clipboard.setText(AnnotatedString(content))
                            menuOpen = false
                        },
                    )
                    if (onReply != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_reply)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Reply, null) },
                            onClick = {
                                onReply(content)
                                menuOpen = false
                            },
                        )
                    }
                }
            }
            if (!isStreaming && actions != null) {
                ReplyActionRow(content = content, actions = actions, modifier = Modifier.offset(x = (-12).dp))
            }
        }
    }
}

@Composable
private fun UserMessageMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onEdit: (() -> Unit)?,
    onReply: (() -> Unit)?,
    onShare: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_copy)) },
            leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
            onClick = {
                onCopy()
                onDismiss()
            },
        )
        if (onEdit != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_edit_message)) },
                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                onClick = {
                    onEdit()
                    onDismiss()
                },
            )
        }
        if (onReply != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_reply)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Reply, null) },
                onClick = {
                    onReply()
                    onDismiss()
                },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_share)) },
            leadingIcon = { Icon(Icons.Rounded.Share, null) },
            onClick = {
                onShare()
                onDismiss()
            },
        )
    }
}

/** Small gradient Lumina mark + model name, like the sparkle header in Gemini / Claude replies. */
@Composable
fun AssistantHeader(modelLabel: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(BrandColors.Indigo, BrandColors.Coral))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Text(
            modelLabel ?: stringResource(R.string.app_name),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun shareText(context: android.content.Context, text: String) {
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    context.startActivity(Intent.createChooser(send, null))
}

@Composable
private fun AssistantMarkdownText(
    content: String,
    showCursor: Boolean,
) {
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
                CodeBlock(part)
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

/** Fenced code with a language label and a tap-to-copy button that confirms with a check mark. */
@Composable
private fun CodeBlock(fenced: String) {
    val clipboard = LocalClipboardManager.current
    val raw = fenced.trim('\n')
    val lines = raw.lines()
    val firstLine = lines.firstOrNull().orEmpty().trim()
    val hasLanguage = lines.size > 1 && firstLine.isNotEmpty() && !firstLine.contains(' ')
    val language = if (hasLanguage) firstLine else ""
    val code = if (hasLanguage) lines.drop(1).joinToString("\n") else raw.trim()
    var copied by remember(code) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1_800)
            copied = false
        }
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    language.ifEmpty { stringResource(R.string.chat_code) },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    },
                ) {
                    Icon(
                        if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        stringResource(if (copied) R.string.chat_copied else R.string.chat_copy_code),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            Text(
                text = code,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false,
            )
        }
    }
}

private val UserBubbleShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 6.dp, bottomStart = 22.dp)

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
