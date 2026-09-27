package com.skillmcp.mentor.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingSendLogicTest {
    private val pending = PendingSend(userMessageId = "u1", replyId = "r1", text = "My tests fail")

    @Test
    fun pendingShownUntilPersistedRowArrives() {
        assertEquals("My tests fail", PendingSendLogic.visiblePendingText(pending, setOf("a", "b")))
    }

    @Test
    fun pendingHiddenOnceSaved_soMessageAppearsExactlyOnce() {
        assertNull(PendingSendLogic.visiblePendingText(pending.copy(saved = true), setOf("a", "u1")))
    }

    @Test
    fun noPendingMeansNothingShown() {
        assertNull(PendingSendLogic.visiblePendingText(null, emptySet()))
    }

    @Test
    fun replySlotShownWhileSendingUntilReplyPersisted() {
        assertTrue(PendingSendLogic.showReplySlot(isSending = true, pending = pending, persistedIds = setOf("u1")))
        assertFalse(PendingSendLogic.showReplySlot(isSending = true, pending = pending, persistedIds = setOf("u1", "r1")))
        assertFalse(PendingSendLogic.showReplySlot(isSending = false, pending = pending, persistedIds = emptySet()))
    }

    @Test
    fun failedSendRestoresTextIntoEmptyComposer() {
        assertEquals("My tests fail", PendingSendLogic.draftAfterFailure("", "My tests fail"))
        assertEquals("My tests fail", PendingSendLogic.draftAfterFailure("   ", "My tests fail"))
    }

    @Test
    fun failedSendKeepsTextTypedDuringRequest() {
        assertEquals("My tests fail\nalso this", PendingSendLogic.draftAfterFailure("also this", "My tests fail"))
    }

    @Test
    fun failedSendDoesNotDuplicateIdenticalDraft() {
        assertEquals("My tests fail", PendingSendLogic.draftAfterFailure("My tests fail", "My tests fail"))
    }
}
