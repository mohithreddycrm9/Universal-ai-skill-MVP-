package com.skillmcp.mentor.ui.chat

import android.net.Uri
import com.skillmcp.mentor.data.ChatScrollPosition
import com.skillmcp.mentor.mentor.PopularUseCase

/**
 * Everything the chat screen can ask for. Implemented by [com.skillmcp.mentor.ui.MentorViewModel];
 * [PreviewChatActions] lets previews and snapshot tests render the real chat UI without a ViewModel.
 */
interface ChatScreenActions {
    fun onDraftChange(value: String)
    fun sendMessage()
    fun cancelSend()
    fun applySuggestion(prompt: String)
    fun startPopularUseCase(useCase: PopularUseCase)
    fun attachFromUri(uri: Uri, mimeType: String?)
    fun clearAttachment()
    fun toggleListen()
    fun cancelListening()
    fun acceptSharedContent()
    fun declineSharedContent()
    fun searchChats(query: String)
    fun newConversation()
    fun selectConversation(id: String)
    fun deleteConversation(id: String)
    fun renameConversation(id: String, name: String)
    fun pinConversation(id: String, pinned: Boolean)
    fun setConversationTag(id: String, tag: String)
    fun shareChatMarkdown(conversationId: String)
    fun shareChatPdf(conversationId: String)
    fun savePrompt(title: String, body: String)
    fun deletePrompt(id: String)
    fun chatScrollFor(conversationId: String): ChatScrollPosition?
    fun saveChatScroll(conversationId: String, position: ChatScrollPosition)
}

/** No-op actions for @Preview / Paparazzi renders of the real chat screen. */
object PreviewChatActions : ChatScreenActions {
    override fun onDraftChange(value: String) = Unit
    override fun sendMessage() = Unit
    override fun cancelSend() = Unit
    override fun applySuggestion(prompt: String) = Unit
    override fun startPopularUseCase(useCase: PopularUseCase) = Unit
    override fun attachFromUri(uri: Uri, mimeType: String?) = Unit
    override fun clearAttachment() = Unit
    override fun toggleListen() = Unit
    override fun cancelListening() = Unit
    override fun acceptSharedContent() = Unit
    override fun declineSharedContent() = Unit
    override fun searchChats(query: String) = Unit
    override fun newConversation() = Unit
    override fun selectConversation(id: String) = Unit
    override fun deleteConversation(id: String) = Unit
    override fun renameConversation(id: String, name: String) = Unit
    override fun pinConversation(id: String, pinned: Boolean) = Unit
    override fun setConversationTag(id: String, tag: String) = Unit
    override fun shareChatMarkdown(conversationId: String) = Unit
    override fun shareChatPdf(conversationId: String) = Unit
    override fun savePrompt(title: String, body: String) = Unit
    override fun deletePrompt(id: String) = Unit
    override fun chatScrollFor(conversationId: String): ChatScrollPosition? = null
    override fun saveChatScroll(conversationId: String, position: ChatScrollPosition) = Unit
}
