package com.skillmcp.mentor.util

import android.net.Uri
import com.skillmcp.mentor.llm.LlmProviderKind
import com.skillmcp.mentor.llm.LanCleartextBridge

object UrlSecurityPolicy {
    /**
     * Backup, sync, and custom API endpoints must use HTTPS.
     * Plain HTTP is only allowed for Ollama base URLs on private/LAN hosts (see [LanCleartextBridge]).
     */
    fun httpsRequiredError(url: String, fieldLabel: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        val scheme = Uri.parse(trimmed).scheme?.lowercase()
        if (scheme == "http" || scheme == "ws") {
            return "$fieldLabel must use a secure connection (https:// or wss://). Plain $scheme:// is not allowed."
        }
        return null
    }

    fun validateLlmBaseUrl(kind: LlmProviderKind, baseUrl: String): String? {
        val trimmed = baseUrl.trim()
        if (trimmed.isBlank()) return null
        val uri = Uri.parse(trimmed)
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return "Base URL must start with https:// or, for Ollama on your LAN, http:// with a private IP."
        }
        if (scheme == "http") {
            if (kind != LlmProviderKind.OLLAMA) {
                return "Only Ollama on a private IP or localhost may use http://. Use https:// for this provider."
            }
            val host = uri.host ?: return "Enter a valid Ollama host (private IP or localhost)."
            val loopback = host.equals("localhost", ignoreCase = true) || host == "127.0.0.1" || host == "10.0.2.2"
            if (!loopback && !LanCleartextBridge.isPrivateIpv4Literal(host)) {
                return "Ollama http:// is only allowed for localhost or a private LAN IP address you control."
            }
        }
        return null
    }
}
