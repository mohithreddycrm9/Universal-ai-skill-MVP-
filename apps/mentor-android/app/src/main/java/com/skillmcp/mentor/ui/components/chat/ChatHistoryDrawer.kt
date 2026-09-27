package com.skillmcp.mentor.ui.components.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.mentor.UiConversation
import java.util.concurrent.TimeUnit

data class ChatHistorySection(
    val title: String,
    val conversations: List<UiConversation>,
)

fun groupConversationsForDrawer(chats: List<UiConversation>, nowMs: Long = System.currentTimeMillis()): List<ChatHistorySection> {
    val pinned = chats.filter { it.pinned }.sortedByDescending { it.updatedAt }
    val rest = chats.filter { !it.pinned }
    val startOfToday = startOfLocalDay(nowMs)
    val startOfYesterday = startOfToday - TimeUnit.DAYS.toMillis(1)
    val today = rest.filter { it.updatedAt >= startOfToday }
    val yesterday = rest.filter { it.updatedAt in startOfYesterday until startOfToday }
    val earlier = rest.filter { it.updatedAt < startOfYesterday }
    return buildList {
        if (pinned.isNotEmpty()) add(ChatHistorySection("Pinned", pinned))
        if (today.isNotEmpty()) add(ChatHistorySection("Today", today.sortedByDescending { it.updatedAt }))
        if (yesterday.isNotEmpty()) add(ChatHistorySection("Yesterday", yesterday.sortedByDescending { it.updatedAt }))
        if (earlier.isNotEmpty()) add(ChatHistorySection("Earlier", earlier.sortedByDescending { it.updatedAt }))
    }
}

private fun startOfLocalDay(nowMs: Long): Long {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = nowMs
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@Composable
fun ChatHistoryDrawer(
    query: String,
    onQueryChange: (String) -> Unit,
    sections: List<ChatHistorySection>,
    activeId: String,
    onNewChat: () -> Unit,
    onOpenChat: (UiConversation) -> Unit,
    conversationRow: @Composable (UiConversation, Boolean, () -> Unit) -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.88f),
        drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
    ) {
        Column(Modifier.fillMaxHeight().padding(vertical = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search") },
                singleLine = true,
            )
            NavigationDrawerItem(
                label = { Text("New chat", fontWeight = FontWeight.Medium) },
                selected = false,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = onNewChat,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                sections.forEach { section ->
                    item("header-${section.title}") {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                        )
                    }
                    items(section.conversations, key = { it.id }) { chat ->
                        conversationRow(chat, chat.id == activeId) { onOpenChat(chat) }
                    }
                }
            }
        }
    }
}
