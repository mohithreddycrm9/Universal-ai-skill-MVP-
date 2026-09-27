package com.skillmcp.mentor.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.ui.MentorTab
import com.skillmcp.mentor.ui.components.motion.pressableScale

@Composable
fun FloatingMentorNavBar(
    selectedRoute: String,
    onSelect: (MentorTab) -> Unit,
    tabLabel: @Composable (MentorTab) -> String,
    modifier: Modifier = Modifier,
) {
    val tabs = MentorTab.entries.filter { it.showInBar }
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            tonalElevation = 3.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab.route == selectedRoute
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "tabBounce",
                    )
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .scale(scale)
                                .pressableScale()
                                .clickable(role = Role.Tab) { onSelect(tab) }
                                .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Box(
                                modifier =
                                    Modifier
                                        .matchParentSize()
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            RoundedCornerShape(20.dp),
                                        ),
                            )
                        }
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                tabNavIcon(tab),
                                contentDescription = tabLabel(tab),
                                modifier = Modifier.size(22.dp),
                                tint =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                            if (isSelected) {
                                Text(
                                    tabLabel(tab),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun tabNavIcon(tab: MentorTab): ImageVector =
    when (tab) {
        MentorTab.Chat -> Icons.AutoMirrored.Filled.Chat
        MentorTab.Discover -> Icons.Outlined.Explore
        MentorTab.Settings -> Icons.Outlined.Settings
        else -> Icons.Outlined.Explore
    }
