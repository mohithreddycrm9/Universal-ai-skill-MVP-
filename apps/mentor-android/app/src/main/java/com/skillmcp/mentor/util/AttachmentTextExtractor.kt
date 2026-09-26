package com.skillmcp.mentor.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object AttachmentTextExtractor {
    suspend fun describeForModel(context: Context, uri: Uri, mimeType: String?): String =
        withContext(Dispatchers.IO) {
            when {
                mimeType?.startsWith("image/") == true -> describeImage(context, uri, mimeType)
                mimeType == "application/pdf" -> describePdf(context, uri)
                else -> {
                    val text = readText(context, uri).take(8_000)
                    if (text.isBlank()) "Attached file ($mimeType)"
                    else "Attached text:\n$text"
                }
            }
        }

    private fun describeImage(context: Context, uri: Uri, mimeType: String): String {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val raw = stream.readBytes()
            val bytes = if (raw.size > 900_000) raw.copyOf(900_000) else raw
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            val dims = "${opts.outWidth}x${opts.outHeight}"
            return "[Image attachment $dims, $mimeType, base64:$b64]\n" +
                "Describe this image for the user and answer their question about it."
        }
        return "[Image attachment could not be read]"
    }

    private fun describePdf(context: Context, uri: Uri): String {
        val raw = context.contentResolver.openInputStream(uri)?.readBytes() ?: return "[PDF unreadable]"
        val bytes = if (raw.size > 200_000) raw.copyOf(200_000) else raw
        val sample = String(bytes, Charsets.ISO_8859_1).take(4_000)
        return "[PDF attachment, ${bytes.size} bytes, text sample:]\n$sample"
    }

    private fun readText(context: Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).readText()
        } ?: ""
}
