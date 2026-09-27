package com.skillmcp.mentor.ui.components.chat

import com.skillmcp.mentor.mentor.UiConversation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ChatHistoryDrawerTest {
    @Test
    fun groupsPinnedTodayYesterdayEarlier() {
        val now = System.currentTimeMillis()
        val chats =
            listOf(
                UiConversation("1", "Pinned", now, pinned = true),
                UiConversation("2", "Today", now - TimeUnit.HOURS.toMillis(1)),
                UiConversation("3", "Yesterday", now - TimeUnit.DAYS.toMillis(1) - 1),
                UiConversation("4", "Old", now - TimeUnit.DAYS.toMillis(3)),
            )
        val sections = groupConversationsForDrawer(chats, now)
        assertEquals(listOf("Pinned", "Today", "Yesterday", "Earlier"), sections.map { it.title })
        assertTrue(sections.first().conversations.any { it.id == "1" })
    }
}
