package com.skillmcp.mentor.mentor

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechTextTest {
    @Test
    fun stripsMarkdownSyntaxForReadAloud() {
        val md = "## Plan\n- **Pack** light\n1. Visit [Fort Aguada](https://x.y)\n> Tip: go early"
        assertEquals("Plan\nPack light\nVisit Fort Aguada\nTip: go early", SpeechText.fromMarkdown(md))
    }

    @Test
    fun codeBlocksAreNotReadOut() {
        assertEquals("Run this:\n(code)\nDone.", SpeechText.fromMarkdown("Run this:\n```kotlin\nval x = 1\n```\nDone."))
    }
}
