package com.skillmcp.mentor.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SharePayload(
    val text: String,
    /** True when content will be sent to the user's configured LLM provider. */
    val sendsToAiProvider: Boolean,
    /** content:// URI of a shared image (attached to the next message after consent). */
    val imageUri: String? = null,
    val imageMimeType: String? = null,
)

class ShareTextHolder {
    private val _pending = MutableStateFlow<SharePayload?>(null)
    val pending: StateFlow<SharePayload?> = _pending.asStateFlow()

    fun push(payload: SharePayload) {
        _pending.value = payload.copy(text = payload.text.trim())
    }

    fun consume(): SharePayload? {
        val value = _pending.value
        _pending.value = null
        return value
    }
}
