package com.skillmcp.mentor.llm

import android.app.Application
import com.skillmcp.mentor.mentor.ChatMessageDto
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** One neutral history, converted for every provider a chat can switch to. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class ProviderMessagesTest {
    private val history =
        listOf(
            ChatMessageDto("user", "Plan a trip to Goa"),
            ChatMessageDto("assistant", "Day 1: beaches"),
        )

    @Test
    fun openAiAndOllamaGetSystemThenTurnsThenUser() {
        val arr = ProviderMessages.openAi("Be brief", history, "Cheaper?", null)
        assertEquals(4, arr.length())
        assertEquals(listOf("system", "user", "assistant", "user"), (0 until arr.length()).map { arr.getJSONObject(it).getString("role") })
        assertEquals("Day 1: beaches", arr.getJSONObject(2).getString("content"))
        assertEquals("Cheaper?", arr.getJSONObject(3).getString("content"))
    }

    @Test
    fun anthropicUsesTextBlocksAndNoSystemMessage() {
        val arr = ProviderMessages.anthropic(history, "Cheaper?", null)
        assertEquals(listOf("user", "assistant", "user"), (0 until arr.length()).map { arr.getJSONObject(it).getString("role") })
        assertEquals("Day 1: beaches", arr.getJSONObject(1).getJSONArray("content").getJSONObject(0).getString("text"))
    }

    @Test
    fun geminiCallsTheAssistantModel() {
        val arr = ProviderMessages.gemini(history, "Cheaper?", null)
        assertEquals(listOf("user", "model", "user"), (0 until arr.length()).map { arr.getJSONObject(it).getString("role") })
        assertEquals("Cheaper?", arr.getJSONObject(2).getJSONArray("parts").getJSONObject(0).getString("text"))
    }

    @Test
    fun imageGoesOnlyOnTheNewUserTurn() {
        val img = ChatVisionAttachment("AAAA")
        val oa = ProviderMessages.openAi("s", history, "What is this?", img)
        assertEquals(2, oa.getJSONObject(3).getJSONArray("content").length())
        val an = ProviderMessages.anthropic(history, "What is this?", img)
        assertEquals("image", an.getJSONObject(2).getJSONArray("content").getJSONObject(0).getString("type"))
    }
}
