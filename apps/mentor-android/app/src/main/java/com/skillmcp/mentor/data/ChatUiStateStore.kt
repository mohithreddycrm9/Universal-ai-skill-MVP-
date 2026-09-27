package com.skillmcp.mentor.data

import android.content.Context
import android.content.SharedPreferences

/** Saved list position for one conversation. [atBottom] means "follow the latest message". */
data class ChatScrollPosition(
    val index: Int,
    val offset: Int,
    val atBottom: Boolean,
)

/**
 * Per-conversation composer draft and scroll position, persisted across process death.
 * Kept out of the main settings DataStore because it is written on every keystroke/scroll stop.
 */
class ChatUiStateStore(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences(FILE, Context.MODE_PRIVATE))

    fun draft(conversationId: String): String = prefs.getString(draftKey(conversationId), "") ?: ""

    fun setDraft(conversationId: String, text: String) {
        if (conversationId.isBlank()) return
        prefs.edit().apply {
            if (text.isBlank()) remove(draftKey(conversationId)) else putString(draftKey(conversationId), text)
        }.apply()
    }

    fun scroll(conversationId: String): ChatScrollPosition? {
        val key = scrollKey(conversationId)
        if (!prefs.contains("$key.i")) return null
        return ChatScrollPosition(
            index = prefs.getInt("$key.i", 0),
            offset = prefs.getInt("$key.o", 0),
            atBottom = prefs.getBoolean("$key.b", true),
        )
    }

    fun setScroll(conversationId: String, position: ChatScrollPosition) {
        if (conversationId.isBlank()) return
        val key = scrollKey(conversationId)
        prefs.edit()
            .putInt("$key.i", position.index.coerceAtLeast(0))
            .putInt("$key.o", position.offset.coerceAtLeast(0))
            .putBoolean("$key.b", position.atBottom)
            .apply()
    }

    fun clear(conversationId: String) {
        val key = scrollKey(conversationId)
        prefs.edit()
            .remove(draftKey(conversationId))
            .remove("$key.i")
            .remove("$key.o")
            .remove("$key.b")
            .apply()
    }

    private fun draftKey(id: String) = "draft.$id"

    private fun scrollKey(id: String) = "scroll.$id"

    companion object {
        const val FILE = "chat_ui_state"
    }
}
