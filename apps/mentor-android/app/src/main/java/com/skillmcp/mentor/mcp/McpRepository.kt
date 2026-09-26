package com.skillmcp.mentor.mcp

import com.skillmcp.mentor.data.LlmSecureStore
import com.skillmcp.mentor.data.db.McpServerEntity
import com.skillmcp.mentor.data.db.MentorDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

data class McpServer(
    val id: String,
    val name: String,
    val endpointUrl: String,
    val enabled: Boolean,
    val hasToken: Boolean,
)

class McpRepository(
    private val dao: MentorDao,
    private val secureStore: LlmSecureStore,
    private val client: McpClient = McpClient(),
) {
    fun observeServers(): Flow<List<McpServer>> =
        dao.observeMcpServers().map { rows ->
            rows.map { row ->
                McpServer(
                    id = row.id,
                    name = row.name,
                    endpointUrl = row.endpointUrl,
                    enabled = row.enabled,
                    hasToken = secureStore.getMcpToken(row.id).isNotBlank(),
                )
            }
        }

    suspend fun upsertServer(name: String, endpointUrl: String, token: String, enabled: Boolean = true): String =
        withContext(Dispatchers.IO) {
            val id = "mcp-${UUID.randomUUID()}"
            dao.upsertMcpServer(
                McpServerEntity(
                    id = id,
                    name = name.trim().ifBlank { "MCP server" },
                    endpointUrl = endpointUrl.trim(),
                    enabled = enabled,
                    addedAt = System.currentTimeMillis(),
                ),
            )
            if (token.isNotBlank()) secureStore.setMcpToken(id, token)
            id
        }

    suspend fun setServerEnabled(id: String, enabled: Boolean) {
        val row = dao.getMcpServer(id) ?: return
        dao.upsertMcpServer(row.copy(enabled = enabled))
    }

    suspend fun deleteServer(id: String) {
        dao.deleteMcpServer(id)
        secureStore.deleteMcpToken(id)
    }

    suspend fun testServer(id: String): Result<String> =
        withContext(Dispatchers.IO) {
            val row = dao.getMcpServer(id) ?: return@withContext Result.failure(IllegalArgumentException("Server not found"))
            val token = secureStore.getMcpToken(row.id)
            client.listTools(row.endpointUrl, token).map { tools ->
                if (tools.isEmpty()) "Connected (no tools listed)" else "Connected · ${tools.size} tools"
            }
        }

    suspend fun buildMcpContext(userMessage: String): String =
        withContext(Dispatchers.IO) {
            val servers = dao.enabledMcpServers()
            if (servers.isEmpty()) return@withContext ""
            val sections = mutableListOf<String>()
            val invocation = parseMcpInvocation(userMessage)
            for (server in servers) {
                val token = secureStore.getMcpToken(server.id)
                val tools = client.listTools(server.endpointUrl, token).getOrElse { emptyList() }
                if (tools.isNotEmpty()) {
                    sections +=
                        buildString {
                            append("### MCP server: ${server.name}\n")
                            tools.take(24).forEach { tool ->
                                append("- `${tool.name}`: ${tool.description.take(200)}\n")
                            }
                            append("\nTo run a tool, send: `/mcp ${server.name} tool_name {\"key\":\"value\"}`")
                        }
                }
                if (invocation != null && invocation.serverHint.equals(server.name, ignoreCase = true)) {
                    val args = invocation.argumentsJson?.let { JSONObject(it) } ?: JSONObject()
                    val output =
                        client.callTool(server.endpointUrl, token, invocation.toolName, args)
                            .getOrElse { "MCP call failed: ${it.message}" }
                    sections += "## MCP tool result (${invocation.toolName})\n$output"
                }
            }
            if (invocation != null && sections.none { it.startsWith("## MCP tool result") }) {
                sections += "## MCP\nNo matching enabled server for `${invocation.serverHint}`."
            }
            sections.joinToString("\n\n")
        }

    private data class McpInvocation(
        val serverHint: String,
        val toolName: String,
        val argumentsJson: String?,
    )

    private fun parseMcpInvocation(message: String): McpInvocation? {
        val trimmed = message.trim()
        if (!trimmed.startsWith("/mcp ", ignoreCase = true)) return null
        val rest = trimmed.removePrefix("/mcp").trim()
        val parts = rest.split(Regex("\\s+"), limit = 3)
        if (parts.size < 2) return null
        return McpInvocation(parts[0], parts[1], parts.getOrNull(2))
    }
}
