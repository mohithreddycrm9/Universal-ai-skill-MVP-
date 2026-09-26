package com.skillmcp.mentor.extensions

import com.skillmcp.mentor.mcp.McpRepository
import com.skillmcp.mentor.plugins.PluginRunner

class ExtensionOrchestrator(
    private val pluginRunner: PluginRunner,
    private val mcpRepository: McpRepository,
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
        val mcpContext = mcpRepository.buildMcpContext(userMessage)
        if (mcpContext.isNotBlank()) parts += mcpContext
        return parts.joinToString("\n\n")
    }
}
