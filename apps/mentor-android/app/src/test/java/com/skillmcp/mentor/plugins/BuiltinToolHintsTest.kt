package com.skillmcp.mentor.plugins

import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinToolHintsTest {
    private val runner = PluginRunner()

    @Test
    fun injectsTimeWhenUserAsks() {
        val ctx = BuiltinToolHints.extraContextForMessage("What time is it?", runner)
        assertTrue(ctx.contains("Device date & time"))
    }

    @Test
    fun injectsCalcForPercentOf() {
        val ctx = BuiltinToolHints.extraContextForMessage("15% of 200", runner)
        assertTrue(ctx.contains("Built-in calculator"))
    }
}
