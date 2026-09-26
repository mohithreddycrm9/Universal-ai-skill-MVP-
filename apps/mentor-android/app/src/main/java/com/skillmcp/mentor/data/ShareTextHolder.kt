package com.skillmcp.mentor.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShareTextHolder {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun push(text: String) {
        _pending.value = text.trim()
    }

    fun consume(): String? {
        val value = _pending.value
        _pending.value = null
        return value
    }
}
