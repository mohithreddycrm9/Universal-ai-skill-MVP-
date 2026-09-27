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
}
