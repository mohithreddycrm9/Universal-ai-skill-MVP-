package com.skillmcp.mentor.ui.components

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
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.ui.theme.AppColors
import com.skillmcp.mentor.ui.theme.BubbleShapeAssistant
import com.skillmcp.mentor.ui.theme.BubbleShapeUser

@Composable
fun MessageBubble(
    content: String,
    isUser: Boolean,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val bubbleColor =
        when {
            isUser && dark -> AppColors.UserBubbleDark
            isUser -> AppColors.UserBubbleLight
            dark -> AppColors.AssistantBubbleDark
            else -> AppColors.AssistantBubbleLight
        }
    val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
    val shape = if (isUser) BubbleShapeUser else BubbleShapeAssistant
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = shape,
            color = bubbleColor,
            shadowElevation = if (isUser) 4.dp else 1.dp,
            tonalElevation = 0.dp,
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
