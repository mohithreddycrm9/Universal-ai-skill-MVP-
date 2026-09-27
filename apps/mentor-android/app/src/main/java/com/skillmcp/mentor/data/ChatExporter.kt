package com.skillmcp.mentor.data

import android.content.Context
import com.skillmcp.mentor.R
import com.skillmcp.mentor.data.db.MentorDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatExporter(
    private val dao: MentorDao,
    private val appContext: Context,
) {
    private suspend fun conversation(conversationId: String): Pair<String, List<ExportTurn>> {
        val project = dao.allProjects().find { it.id == conversationId }
        val title = project?.name ?: appContext.getString(R.string.app_name)
        val turns =
            dao.messagesFor(conversationId)
                .filter { it.role == "user" || it.role == "assistant" }
                .map { ExportTurn(it.role, it.content) }
        return title to turns
    }

    suspend fun exportConversationMarkdown(conversationId: String): String =
        withContext(Dispatchers.IO) {
            val (title, turns) = conversation(conversationId)
            val date = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date())
            ChatExportFormat.markdown(
                title = title,
                turns = turns,
                exportedOn = appContext.getString(R.string.chat_export_on, date),
                userLabel = appContext.getString(R.string.chat_export_you),
                assistantLabel = appContext.getString(R.string.app_name),
            )
        }

    suspend fun exportConversationText(conversationId: String): String =
        withContext(Dispatchers.IO) {
            val (title, turns) = conversation(conversationId)
            ChatExportFormat.plainText(
                title = title,
                turns = turns,
                userLabel = appContext.getString(R.string.chat_export_you),
                assistantLabel = appContext.getString(R.string.app_name),
            )
        }

    suspend fun exportAllMarkdown(): String =
        withContext(Dispatchers.IO) {
            val projects = dao.allProjects().sortedBy { it.updatedAt }
            buildString {
                appendLine("# ${appContext.getString(R.string.chat_export_all_header)}")
                appendLine()
                projects.forEach { project ->
                    val messages =
                        dao.allMessages()
                            .filter { it.projectId == project.id && (it.role == "user" || it.role == "assistant") }
                            .sortedBy { it.createdAt }
                    appendLine("## ${project.name}")
                    appendLine()
                    messages.forEach { msg ->
                        appendLine("### ${msg.role}")
                        appendLine(msg.content.trim())
                        appendLine()
                    }
                    appendLine("---")
                    appendLine()
                }
            }
        }
}
