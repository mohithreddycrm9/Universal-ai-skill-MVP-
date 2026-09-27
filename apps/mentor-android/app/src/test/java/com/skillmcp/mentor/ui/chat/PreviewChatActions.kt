package com.skillmcp.mentor.ui.chat

import android.net.Uri
import com.skillmcp.mentor.data.ChatScrollPosition
import com.skillmcp.mentor.mentor.PopularUseCase

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
    override fun requestOpenTab(route: String) = Unit
    override fun dismissSpendBlockMessage() = Unit
    override fun startEdit(messageId: String) = Unit
    override fun cancelEdit() = Unit
    override fun regenerate(messageId: String) = Unit
    override fun selectReplyVersion(messageId: String, index: Int) = Unit
    override fun readAloud(messageId: String, text: String) = Unit
    override fun stopReadAloud() = Unit
    override fun sendFollowUp(prompt: String) = Unit
    override fun shareChatText(conversationId: String) = Unit
}
