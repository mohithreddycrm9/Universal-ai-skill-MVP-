package com.skillmcp.mentor.ui.chat

import android.net.Uri
import com.skillmcp.mentor.data.ChatScrollPosition
import com.skillmcp.mentor.mentor.PopularUseCase

/**
 * Everything the chat screen can ask for. Implemented by [com.skillmcp.mentor.ui.MentorViewModel];
 * Test-only PreviewChatActions (src/test) lets previews and snapshot tests render the real chat UI without a ViewModel.
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
    fun requestOpenTab(route: String)
    fun dismissSpendBlockMessage()
    fun startEdit(messageId: String)
    fun cancelEdit()
    fun regenerate(messageId: String)
    fun selectReplyVersion(messageId: String, index: Int)
    fun readAloud(messageId: String, text: String)
    fun stopReadAloud()
    fun sendFollowUp(prompt: String)
    fun shareChatText(conversationId: String)
    fun switchChatModel(profileId: String)
    fun connectModel(profileId: String)
}
