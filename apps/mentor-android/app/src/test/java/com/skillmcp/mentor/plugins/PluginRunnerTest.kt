package com.skillmcp.mentor.plugins

import org.junit.Assert.assertTrue
import org.junit.Test

class PluginRunnerTest {
    private val runner = PluginRunner()

    @Test
    fun calcEvaluatesExpressionWhenBuiltIn() {
        val out = runner.runCommands("/calc 2 + 3 * 4", emptySet())
        assertTrue(out.contains("14"))
    }

    @Test
    fun uuidWhenBuiltInWithoutToggle() {
        val out = runner.runCommands("/uuid", emptySet())
        assertTrue(out.contains("-"))
    }

    @Test
    fun fetchRequiresEnable() {
        val disabled = runner.runCommands("/fetch https://example.com", emptySet())
        assertTrue(disabled.isBlank())
    }
}
