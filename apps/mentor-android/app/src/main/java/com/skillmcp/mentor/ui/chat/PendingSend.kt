package com.skillmcp.mentor.ui.chat

/**
 * A message the user just sent. [userMessageId] and [replyId] are generated before the request so
 * the optimistic bubble, the "Thinking…" row, the streaming reply and the persisted rows all share
 * one stable LazyColumn key each (no swap, no layout jump).
 */
data class PendingSend(
    val userMessageId: String,
    val replyId: String,
    val text: String,
    val saved: Boolean = false,
)

object PendingSendLogic {
    /** The optimistic copy is shown only until the persisted row with the same id arrives. */
    fun visiblePendingText(pending: PendingSend?, persistedIds: Set<String>): String? =
        pending?.takeUnless { it.userMessageId in persistedIds }?.text

    /** The streaming/thinking row is shown only until the persisted reply with the same id arrives. */
    fun showReplySlot(isSending: Boolean, pending: PendingSend?, persistedIds: Set<String>): Boolean =
        isSending && (pending == null || pending.replyId !in persistedIds)

    /**
     * Text to put back in the composer after a failed or blocked send. Never discards what the user
     * typed while the request was running.
     */
    fun draftAfterFailure(currentDraft: String, sentText: String): String =
        when {
            currentDraft.isBlank() -> sentText
            currentDraft.trim() == sentText.trim() -> currentDraft
            else -> sentText + "\n" + currentDraft
        }
}
