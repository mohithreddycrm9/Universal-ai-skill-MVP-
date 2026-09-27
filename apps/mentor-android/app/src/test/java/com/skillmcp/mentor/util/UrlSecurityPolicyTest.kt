package com.skillmcp.mentor.util

import com.skillmcp.mentor.llm.LlmProviderKind
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UrlSecurityPolicyTest {
    @Test
    fun backupUrl_rejectsHttp() {
        assertNotNull(UrlSecurityPolicy.httpsRequiredError("http://evil.example/backup", "Backup URL"))
    }

    @Test
    fun backupUrl_allowsHttps() {
        assertNull(UrlSecurityPolicy.httpsRequiredError("https://backup.example/upload", "Backup URL"))
    }

    @Test
    fun ollama_allowsHttpOnPrivateIp() {
        assertNull(UrlSecurityPolicy.validateLlmBaseUrl(LlmProviderKind.OLLAMA, "http://192.168.1.10:11434/"))
    }

    @Test
    fun openAi_rejectsHttp() {
        assertNotNull(UrlSecurityPolicy.validateLlmBaseUrl(LlmProviderKind.OPENAI_COMPAT, "http://api.openai.com/v1/"))
    }

    @Test
    fun syncUrl_rejectsPlainWebSocket() {
        assertNotNull(UrlSecurityPolicy.httpsRequiredError("ws://relay.example/sync", "Sync URL"))
        assertNull(UrlSecurityPolicy.httpsRequiredError("wss://relay.example/sync", "Sync URL"))
    }

    @Test
    fun blankUrlsAreAllowed() {
        assertNull(UrlSecurityPolicy.httpsRequiredError("  ", "Backup URL"))
        assertNull(UrlSecurityPolicy.validateLlmBaseUrl(LlmProviderKind.OLLAMA, ""))
    }

    @Test
    fun ollama_allowsAllPrivateRangesAndLoopback() {
        listOf(
            "http://10.0.0.5:11434/",
            "http://172.20.1.2:11434/",
            "http://192.168.0.10:11434/",
            "http://localhost:11434/",
            "http://127.0.0.1:11434/",
            "http://10.0.2.2:11434/",
        ).forEach { assertNull(it, UrlSecurityPolicy.validateLlmBaseUrl(LlmProviderKind.OLLAMA, it)) }
    }

    @Test
    fun ollama_rejectsHttpToPublicHostsAndHostnames() {
        listOf(
            "http://8.8.8.8:11434/",
            "http://ollama.example.com:11434/",
            "http://192.168.1.10.nip.io:11434/",
            "http://172.32.0.1:11434/",
        ).forEach { assertNotNull(it, UrlSecurityPolicy.validateLlmBaseUrl(LlmProviderKind.OLLAMA, it)) }
    }

    @Test
    fun ollama_allowsHttpsAnywhere() {
        assertNull(UrlSecurityPolicy.validateLlmBaseUrl(LlmProviderKind.OLLAMA, "https://ollama.example.com/"))
    }

    @Test
    fun cloudProviders_rejectHttpEvenOnPrivateIp() {
        listOf(LlmProviderKind.ANTHROPIC, LlmProviderKind.GEMINI, LlmProviderKind.HUGGING_FACE).forEach {
            assertNotNull(it.name, UrlSecurityPolicy.validateLlmBaseUrl(it, "http://192.168.1.10/v1/"))
        }
    }

    @Test
    fun nonHttpSchemesAreRejected() {
        assertNotNull(UrlSecurityPolicy.validateLlmBaseUrl(LlmProviderKind.OLLAMA, "ftp://192.168.1.10/"))
        assertNotNull(UrlSecurityPolicy.validateLlmBaseUrl(LlmProviderKind.OPENAI_COMPAT, "file:///etc/hosts"))
    }
}
