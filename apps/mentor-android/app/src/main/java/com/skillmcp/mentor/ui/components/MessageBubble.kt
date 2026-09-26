package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.ui.theme.AppColors
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
    val dark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    val bubbleColor =
        when {
            isUser && dark -> AppColors.ChatUserDark
            isUser -> AppColors.ChatUserLight
            dark -> AppColors.ChatAssistantDark
            else -> AppColors.ChatAssistantLight
        }
    val textColor =
        when {
            isUser && dark -> AppColors.ChatUserOnDark
            isUser -> AppColors.ChatUserOnLight
            dark -> AppColors.ChatAssistantOnDark
            else -> AppColors.ChatAssistantOnLight
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
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            border =
                if (!isUser) {
                    BorderStroke(1.dp, scheme.outlineVariant)
                } else {
                    null
                },
        ) {
            Text(
                text = content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
            )
        }
    }
}
