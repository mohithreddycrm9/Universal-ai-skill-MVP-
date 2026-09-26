package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.ui.theme.BubbleShapeAssistant
import com.skillmcp.mentor.ui.theme.BubbleShapeUser

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    content: String,
    isUser: Boolean,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val scheme = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val bubbleColor =
        when {
            isUser -> scheme.primary
            dark -> scheme.surfaceContainerHighest
            else -> scheme.primaryContainer.copy(alpha = 0.55f)
        }
    val textColor =
        when {
            isUser -> scheme.onPrimary
            dark -> scheme.onSurface
            else -> scheme.onPrimaryContainer
        }
    val shape = if (isUser) BubbleShapeUser else BubbleShapeAssistant
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(0.9f)
                    .combinedClickable(onClick = {}, onLongClick = { clipboard.setText(AnnotatedString(content)) }),
            shape = shape,
            color = bubbleColor,
            shadowElevation = if (isUser) 6.dp else 2.dp,
            tonalElevation = 0.dp,
            border =
                if (!isUser) {
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        scheme.outlineVariant.copy(alpha = 0.5f),
                    )
                } else {
                    null
                },
        ) {
            Text(
                text = content,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
            )
        }
    }
}
