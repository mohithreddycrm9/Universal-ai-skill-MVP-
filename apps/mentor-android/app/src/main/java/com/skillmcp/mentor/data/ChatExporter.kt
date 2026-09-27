package com.skillmcp.mentor.data

import android.content.Context
import com.skillmcp.mentor.R
import com.skillmcp.mentor.data.db.ChatMessageEntity
import com.skillmcp.mentor.data.db.MentorDao
import com.skillmcp.mentor.data.db.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatExporter(
    private val dao: MentorDao,
    private val appContext: Context,
) {
    suspend fun exportConversationMarkdown(conversationId: String): String =
        withContext(Dispatchers.IO) {
            val project = dao.allProjects().find { it.id == conversationId }
            val title = project?.name ?: "Chat"
            val messages = dao.allMessages().filter { it.projectId == conversationId }.sortedBy { it.createdAt }
            buildString {
                appendLine("# $title")
                appendLine()
                appendLine("_Exported ${java.time.Instant.now()}_")
                appendLine()
                messages.forEach { msg ->
                    appendLine("## ${msg.role.replaceFirstChar { it.uppercase() }}")
                    appendLine()
                    appendLine(msg.content.trim())
                    appendLine()
                }
            }
        }

    suspend fun exportAllMarkdown(): String =
        withContext(Dispatchers.IO) {
            val projects = dao.allProjects().sortedBy { it.updatedAt }
            buildString {
                appendLine("# ${appContext.getString(R.string.chat_export_all_header)}")
                appendLine()
                projects.forEach { project ->
                    val messages =
                        dao.allMessages().filter { it.projectId == project.id }.sortedBy { it.createdAt }
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
