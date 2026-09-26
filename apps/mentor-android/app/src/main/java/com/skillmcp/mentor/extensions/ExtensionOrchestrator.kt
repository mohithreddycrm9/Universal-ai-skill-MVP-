package com.skillmcp.mentor.extensions

import com.skillmcp.mentor.plugins.PluginRunner

class ExtensionOrchestrator(
    private val pluginRunner: PluginRunner,
) {
    suspend fun augmentExtraContext(
        userMessage: String,
        skillContext: String,
        enabledPluginIds: Set<String>,
    ): String {
        val parts = mutableListOf<String>()
        if (skillContext.isNotBlank()) parts += skillContext
        val pluginOutput = pluginRunner.runCommands(userMessage, enabledPluginIds)
        if (pluginOutput.isNotBlank()) parts += pluginOutput
        val pluginHelp = pluginRunner.helpText(enabledPluginIds)
        if (pluginHelp.isNotBlank() && !userMessage.trimStart().startsWith("/")) {
            parts += pluginHelp
        }
        return parts.joinToString("\n\n")
    }
}
