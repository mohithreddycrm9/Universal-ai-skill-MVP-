package com.skillmcp.mentor.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.skillmcp.mentor.llm.ChatVisionAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageAttachmentProcessor {
    private const val MAX_EDGE = 1024
    private const val JPEG_QUALITY = 85

    suspend fun fromUri(context: Context, uri: Uri): ChatVisionAttachment? =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val original = BitmapFactory.decodeStream(stream) ?: return@withContext null
                val scaled = scaleDown(original, MAX_EDGE)
                if (scaled != original) original.recycle()
                val bytes = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, bytes)
                scaled.recycle()
                val b64 = android.util.Base64.encodeToString(bytes.toByteArray(), android.util.Base64.NO_WRAP)
                ChatVisionAttachment(jpegBase64 = b64)
            }
        }

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longest = max(w, h)
        if (longest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / longest
        val nw = (w * scale).toInt().coerceAtLeast(1)
        val nh = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, nw, nh, true)
    }
}
