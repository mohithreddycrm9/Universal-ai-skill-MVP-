package com.skillmcp.mentor.data

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.skillmcp.mentor.data.db.MentorDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ChatPdfExporter(
    private val context: Context,
    private val dao: MentorDao,
) {
    suspend fun exportConversationPdf(conversationId: String): File =
        withContext(Dispatchers.IO) {
            val project = dao.allProjects().find { it.id == conversationId }
            val title = project?.name ?: "Chat"
            val messages =
                dao.allMessages()
                    .filter { it.projectId == conversationId && (it.role == "user" || it.role == "assistant") }
                    .sortedBy { it.createdAt }
            val lines = mutableListOf<String>()
            lines += title
            lines += ""
            messages.forEach { msg ->
                lines += "${msg.role.uppercase()}:"
                lines += wrapText(msg.content.trim(), maxChars = 85)
                lines += ""
            }
            val doc = PdfDocument()
            val paint = Paint().apply { textSize = 11f }
            val pageWidth = 595
            val pageHeight = 842
            val margin = 40f
            val lineHeight = 16f
            var y = margin
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = doc.startPage(pageInfo)
            var canvas = page.canvas
            lines.forEach { line ->
                line.split("\n").forEach { sub ->
                    if (y > pageHeight - margin) {
                        doc.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = doc.startPage(pageInfo)
                        canvas = page.canvas
                        y = margin
                    }
                    canvas.drawText(sub, margin, y, paint)
                    y += lineHeight
                }
            }
            doc.finishPage(page)
            val out =
                File(context.cacheDir, "chat-export-${conversationId.take(8)}.pdf").apply {
                    parentFile?.mkdirs()
                }
            FileOutputStream(out).use { doc.writeTo(it) }
            doc.close()
            out
        }

    private fun wrapText(text: String, maxChars: Int): String {
        val words = text.split(Regex("\\s+"))
        val out = StringBuilder()
        var lineLen = 0
        words.forEach { word ->
            val w = word.trim()
            if (w.isEmpty()) return@forEach
            if (lineLen + w.length + 1 > maxChars) {
                out.append('\n')
                lineLen = 0
            }
            if (lineLen > 0) {
                out.append(' ')
                lineLen++
            }
            out.append(w)
            lineLen += w.length
        }
        return out.toString()
    }
}
