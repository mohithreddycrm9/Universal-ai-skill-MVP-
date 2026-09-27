package com.skillmcp.mentor.ui.components.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelSwitcherTest {
    @Test
    fun contextSizesReadNaturally() {
        assertEquals("4K", formatTokens(4_096))
        assertEquals("8K", formatTokens(8_192))
        assertEquals("128K", formatTokens(128_000))
        assertEquals("1M", formatTokens(1_000_000))
        assertEquals("512", formatTokens(512))
    }
}
