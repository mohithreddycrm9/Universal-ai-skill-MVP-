package com.skillmcp.mentor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChatExportFormatTest {
    private val turns =
        listOf(
            ExportTurn("user", "  Plan a Goa trip  "),
            ExportTurn("assistant", "**Day 1:** beaches"),
            ExportTurn("assistant", "   "),
        )

    @Test
    fun markdownHasTitleDateAndLabelledTurns() {
        val md = ChatExportFormat.markdown("Goa trip", turns, "Exported on 27 Sep 2026", "You", "Lumina")
        assertEquals(
            "# Goa trip\n\n_Exported on 27 Sep 2026_\n\n**You**\n\nPlan a Goa trip\n\n**Lumina**\n\n**Day 1:** beaches\n",
            md,
        )
    }

    @Test
    fun plainTextHasNoMarkdownHeadings() {
        val text = ChatExportFormat.plainText("Goa trip", turns, "You", "Lumina")
        assertEquals("Goa trip\n\nYou:\nPlan a Goa trip\n\nLumina:\n**Day 1:** beaches\n", text)
        assertFalse(text.contains("# "))
    }

    @Test
    fun blankTitleFallsBackToAssistantName() {
        assertEquals("Lumina\n", ChatExportFormat.plainText(" ", emptyList(), "You", "Lumina"))
    }
}
