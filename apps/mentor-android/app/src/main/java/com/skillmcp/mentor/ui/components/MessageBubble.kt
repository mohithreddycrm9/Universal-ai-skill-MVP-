package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            text = content,
            modifier =
                Modifier
                    .fillMaxWidth(0.88f)
                    .clip(shape)
                    .background(bubbleColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
        )
    }
}
