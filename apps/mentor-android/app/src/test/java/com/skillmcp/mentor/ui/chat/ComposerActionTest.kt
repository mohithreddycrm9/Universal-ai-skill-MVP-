package com.skillmcp.mentor.ui.chat

import com.skillmcp.mentor.ui.components.chat.ComposerAction
import com.skillmcp.mentor.ui.components.chat.composerAction
import org.junit.Assert.assertEquals
import org.junit.Test

class ComposerActionTest {
    @Test
    fun morphsVoiceSendStop() {
        assertEquals(ComposerAction.VOICE, composerAction(hasText = false, isSending = false))
        assertEquals(ComposerAction.SEND, composerAction(hasText = true, isSending = false))
        assertEquals(ComposerAction.STOP, composerAction(hasText = true, isSending = true))
        assertEquals(ComposerAction.STOP, composerAction(hasText = false, isSending = true))
    }
}
