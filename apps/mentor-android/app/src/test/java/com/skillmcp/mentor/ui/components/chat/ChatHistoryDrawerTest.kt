package com.skillmcp.mentor.ui.components.chat

import com.skillmcp.mentor.mentor.UiConversation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ChatHistoryDrawerTest {
    @Test
    fun groupsPinnedTodayYesterdayEarlier() {
        // Noon today, so "an hour ago" is always still today.
        val now =
            java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 12)
                set(java.util.Calendar.MINUTE, 0)
            }.timeInMillis
        val chats =
            listOf(
                UiConversation("1", "Pinned", now, pinned = true),
                UiConversation("2", "Today", now - TimeUnit.HOURS.toMillis(1)),
                UiConversation("3", "Yesterday", now - TimeUnit.DAYS.toMillis(1) - 1),
                UiConversation("4", "Old", now - TimeUnit.DAYS.toMillis(3)),
            )
        val sections = groupConversationsForDrawer(chats, now)
        assertEquals(
            listOf(DrawerSection.PINNED, DrawerSection.TODAY, DrawerSection.YESTERDAY, DrawerSection.EARLIER),
            sections.map { it.kind },
        )
        assertTrue(sections.first().conversations.any { it.id == "1" })
    }
}
