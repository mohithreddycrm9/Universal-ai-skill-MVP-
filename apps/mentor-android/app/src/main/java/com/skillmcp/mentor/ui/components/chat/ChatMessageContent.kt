package com.skillmcp.mentor.ui.components.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.skillmcp.mentor.ui.theme.BubbleShapeUser
import com.skillmcp.mentor.ui.theme.contentColorOn
import com.skillmcp.mentor.util.SpendFormat

@Composable
fun ChatMessageContent(
    content: String,
    isUser: Boolean,
    isStreaming: Boolean,
    modelLabel: String?,
    estimatedCostUsd: Double?,
    modifier: Modifier = Modifier,
) {
    if (isUser) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Surface(
                shape = BubbleShapeUser,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 2.dp,
            ) {
                Text(
                    text = content,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        return
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AssistantMarkdownText(content = content, showCursor = isStreaming)
        if (!isStreaming && (modelLabel != null || estimatedCostUsd != null)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                modelLabel?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                estimatedCostUsd?.takeIf { it > 0 }?.let { cost ->
                    Text(
                        SpendFormat.formatUsdEstimate(cost),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantMarkdownText(
    content: String,
    showCursor: Boolean,
) {
    val clipboard = LocalClipboardManager.current
    val parts = content.split("```")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
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
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = "Copy code",
                            )
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
