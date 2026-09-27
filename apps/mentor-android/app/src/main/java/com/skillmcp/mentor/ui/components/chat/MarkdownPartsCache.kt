package com.skillmcp.mentor.ui.components.chat

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MarkdownPartsCache {
    private val cache = LruCache<String, List<String>>(96)

    suspend fun splitFencedBlocks(content: String): List<String> =
        withContext(Dispatchers.Default) {
            val key = content
            cache.get(key)?.let { return@withContext it }
            val parts = content.split("```")
            cache.put(key, parts)
            parts
        }
}
