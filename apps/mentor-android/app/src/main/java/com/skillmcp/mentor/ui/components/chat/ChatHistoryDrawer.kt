package com.skillmcp.mentor.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.R
import com.skillmcp.mentor.mentor.MessageSearchHit
import com.skillmcp.mentor.mentor.UiConversation
import com.skillmcp.mentor.ui.theme.BrandColors
import java.util.concurrent.TimeUnit

enum class DrawerSection { PINNED, TODAY, YESTERDAY, EARLIER }

data class ChatHistorySection(
    val kind: DrawerSection,
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
        if (pinned.isNotEmpty()) add(ChatHistorySection(DrawerSection.PINNED, pinned))
        if (today.isNotEmpty()) add(ChatHistorySection(DrawerSection.TODAY, today.sortedByDescending { it.updatedAt }))
        if (yesterday.isNotEmpty()) add(ChatHistorySection(DrawerSection.YESTERDAY, yesterday.sortedByDescending { it.updatedAt }))
        if (earlier.isNotEmpty()) add(ChatHistorySection(DrawerSection.EARLIER, earlier.sortedByDescending { it.updatedAt }))
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
private fun DrawerSection.label(): String =
    stringResource(
        when (this) {
            DrawerSection.PINNED -> R.string.drawer_section_pinned
            DrawerSection.TODAY -> R.string.drawer_section_today
            DrawerSection.YESTERDAY -> R.string.drawer_section_yesterday
            DrawerSection.EARLIER -> R.string.drawer_section_earlier
        },
    )

/** Chat history: brand header, New chat, pill search (titles and message text), grouped chats. */
@Composable
fun ChatHistoryDrawer(
    query: String,
    onQueryChange: (String) -> Unit,
    sections: List<ChatHistorySection>,
    activeId: String,
    onNewChat: () -> Unit,
    onOpenChat: (UiConversation) -> Unit,
    conversationRow: @Composable (UiConversation, Boolean, () -> Unit) -> Unit,
    footer: @Composable () -> Unit = {},
    messageHits: List<MessageSearchHit> = emptyList(),
    onOpenHit: (MessageSearchHit) -> Unit = {},
) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.88f),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
    ) {
        Column(Modifier.fillMaxHeight().padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(BrandColors.Indigo, BrandColors.Coral))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() },
                )
            }
            FilledTonalButton(
                onClick = onNewChat,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 48.dp),
            ) {
                Icon(Icons.Rounded.EditNote, contentDescription = null)
                Text(stringResource(R.string.drawer_new_chat), modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
            }
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.drawer_search)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.drawer_clear_search))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
            )
            LazyColumn(
                modifier = Modifier.weight(1f).padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (sections.isEmpty() && messageHits.isEmpty()) {
                    item("empty") {
                        Text(
                            if (query.isBlank()) stringResource(R.string.drawer_empty) else stringResource(R.string.drawer_no_results, query),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                        )
                    }
                }
                sections.forEach { section ->
                    item("header-${section.kind}") { SectionHeader(section.kind.label()) }
                    items(section.conversations, key = { it.id }) { chat ->
                        conversationRow(chat, chat.id == activeId) { onOpenChat(chat) }
                    }
                }
                if (messageHits.isNotEmpty()) {
                    item("header-messages") { SectionHeader(stringResource(R.string.drawer_section_messages)) }
                    items(messageHits, key = { "hit-${it.messageId}" }) { hit ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOpenHit(hit) }
                                .heightIn(min = 56.dp)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(hit.conversationName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                hit.snippet,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            footer()
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 6.dp).semantics { heading() },
    )
}
