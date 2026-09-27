package com.skillmcp.mentor.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class ChatUiStateStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(ChatUiStateStore.FILE, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun draftIsPerConversationAndSurvivesNewInstance() {
        ChatUiStateStore(context).apply {
            setDraft("a", "draft for A")
            setDraft("b", "draft for B")
        }
        val reloaded = ChatUiStateStore(context)
        assertEquals("draft for A", reloaded.draft("a"))
        assertEquals("draft for B", reloaded.draft("b"))
        assertEquals("", reloaded.draft("c"))
    }

    @Test
    fun blankDraftRemovesEntry() {
        val store = ChatUiStateStore(context)
        store.setDraft("a", "x")
        store.setDraft("a", "  ")
        assertEquals("", store.draft("a"))
    }

    @Test
    fun scrollPositionRoundTripsPerConversation() {
        val store = ChatUiStateStore(context)
        assertNull(store.scroll("a"))
        store.setScroll("a", ChatScrollPosition(index = 7, offset = 42, atBottom = false))
        store.setScroll("b", ChatScrollPosition(index = 3, offset = 0, atBottom = true))
        val reloaded = ChatUiStateStore(context)
        assertEquals(ChatScrollPosition(7, 42, false), reloaded.scroll("a"))
        assertEquals(ChatScrollPosition(3, 0, true), reloaded.scroll("b"))
    }

    @Test
    fun clearRemovesDraftAndScroll() {
        val store = ChatUiStateStore(context)
        store.setDraft("a", "x")
        store.setScroll("a", ChatScrollPosition(1, 2, false))
        store.clear("a")
        assertEquals("", store.draft("a"))
        assertNull(store.scroll("a"))
    }

    @Test
    fun blankConversationIdIsIgnored() {
        val store = ChatUiStateStore(context)
        store.setDraft("", "x")
        store.setScroll("", ChatScrollPosition(1, 1, false))
        assertEquals("", store.draft(""))
        assertNull(store.scroll(""))
    }
}
