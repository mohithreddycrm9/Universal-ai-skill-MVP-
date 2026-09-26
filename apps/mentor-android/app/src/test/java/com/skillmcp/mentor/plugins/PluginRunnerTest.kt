package com.skillmcp.mentor.plugins

import org.junit.Assert.assertTrue
import org.junit.Test

class PluginRunnerTest {
    private val runner = PluginRunner()

    @Test
    fun calcEvaluatesExpression() {
        val out = runner.runCommands("/calc 2 + 3 * 4", setOf("calc"))
        assertTrue(out.contains("14"))
    }

    @Test
    fun uuidWhenEnabled() {
        val out = runner.runCommands("/uuid", setOf("uuid"))
        assertTrue(out.contains("-"))
    }
}
