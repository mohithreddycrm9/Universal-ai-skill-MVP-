package com.skillmcp.mentor.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

object PdfTextExtractor {
    suspend fun extractText(context: Context, uri: Uri, maxPages: Int = 8): String =
        withContext(Dispatchers.IO) {
            val cache = File(context.cacheDir, "pdf-attach-${uri.hashCode()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                cache.outputStream().use { out -> input.copyTo(out) }
            } ?: return@withContext ""
            val embedded = extractEmbeddedText(context, cache, maxPages)
            if (embedded.isNotBlank()) return@withContext embedded.take(12_000)
            ocrPdfPages(context, cache, maxPages)
        }

    private fun extractEmbeddedText(context: Context, file: File, maxPages: Int): String {
        return runCatching {
            PDFBoxResourceLoader.init(context.applicationContext)
            PDDocument.load(file).use { doc ->
                val stripper = PDFTextStripper()
                stripper.startPage = 1
                stripper.endPage = minOf(doc.numberOfPages, maxPages)
                stripper.getText(doc).trim()
            }
        }.getOrDefault("")
    }

    private suspend fun ocrPdfPages(context: Context, cache: File, maxPages: Int): String =
        ParcelFileDescriptor.open(cache, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val parts = mutableListOf<String>()
                val pages = minOf(renderer.pageCount, maxPages)
                for (i in 0 until pages) {
                    renderer.openPage(i).use { page ->
                        val bitmap =
                            Bitmap.createBitmap(
                                page.width.coerceAtMost(1600),
                                page.height.coerceAtMost(1600),
                                Bitmap.Config.ARGB_8888,
                            )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val text = recognizeBitmap(recognizer, bitmap)
                        bitmap.recycle()
                        if (text.isNotBlank()) parts += "Page ${i + 1}:\n$text"
                    }
                }
                recognizer.close()
                parts.joinToString("\n\n").take(12_000)
            }
        }

    private suspend fun recognizeBitmap(
        recognizer: com.google.mlkit.vision.text.TextRecognizer,
        bitmap: Bitmap,
    ): String =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result -> cont.resume(result.text) }
                .addOnFailureListener { cont.resume("") }
        }
}
