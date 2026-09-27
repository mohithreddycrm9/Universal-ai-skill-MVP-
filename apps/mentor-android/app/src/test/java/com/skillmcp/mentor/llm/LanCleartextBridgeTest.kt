package com.skillmcp.mentor.llm

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

class LanCleartextBridgeTest {
    @Before
    fun reset() = LanCleartextBridge.clearForTest()

    @Test
    fun privateRangesAreRecognised() {
        listOf("10.0.0.5", "172.16.0.1", "172.31.255.254", "192.168.1.42").forEach {
            assertTrue(it, LanCleartextBridge.isPrivateIpv4Literal(it))
        }
    }

    @Test
    fun publicAndMalformedHostsAreRejected() {
        listOf(
            "8.8.8.8", "172.32.0.1", "172.15.0.1", "192.169.0.1", "100.64.0.1",
            "192.168.1", "192.168.1.256", "192.168.01.2", "api.openai.com", "192.168.1.42.nip.io", "",
        ).forEach { assertFalse(it, LanCleartextBridge.isPrivateIpv4Literal(it)) }
    }

    @Test
    fun encodeDecodeRoundTrip() {
        val host = LanCleartextBridge.encodeHost("192.168.1.42")
        assertEquals("192-168-1-42.ollama-lan.invalid", host)
        assertEquals("192.168.1.42", LanCleartextBridge.decodeHost(host))
    }

    @Test
    fun decodeRejectsPublicIpsAndOtherZones() {
        assertNull(LanCleartextBridge.decodeHost("8-8-8-8.ollama-lan.invalid"))
        assertNull(LanCleartextBridge.decodeHost("192-168-1-42.example.com"))
        assertNull(LanCleartextBridge.decodeHost("ollama-lan.invalid"))
    }

    @Test
    fun httpPrivateIpIsBridgedWithOriginalHostHeader() {
        val req = Request.Builder().url("http://192.168.1.42:11434/api/chat").build()
        val bridged = LanCleartextBridge.bridge(req)
        assertEquals("192-168-1-42.ollama-lan.invalid", bridged.url.host)
        assertEquals(11434, bridged.url.port)
        assertEquals("/api/chat", bridged.url.encodedPath)
        assertEquals("192.168.1.42:11434", bridged.header("Host"))
        assertTrue(LanCleartextBridge.isRegistered("192.168.1.42"))
    }

    @Test
    fun httpsLoopbackAndPublicAreNotBridged() {
        listOf(
            "https://192.168.1.42:11434/api/chat",
            "http://localhost:11434/api/chat",
            "http://127.0.0.1:11434/api/chat",
            "http://10.0.2.2:11434/api/chat",
            "http://8.8.8.8/api/chat",
            "http://ollama.example.com/api/chat",
        ).forEach { url ->
            assertFalse(url, LanCleartextBridge.needsBridge(url.toHttpUrl()))
            val req = Request.Builder().url(url).build()
            assertEquals(url, LanCleartextBridge.bridge(req).url.toString())
        }
        assertFalse(LanCleartextBridge.isRegistered("8.8.8.8"))
    }

    @Test
    fun dnsResolvesOnlyRegisteredPrivateIps() {
        LanCleartextBridge.bridge(Request.Builder().url("http://192.168.1.42:11434/").build())
        val resolved = LanCleartextBridge.dns.lookup("192-168-1-42.ollama-lan.invalid")
        assertEquals("192.168.1.42", resolved.single().hostAddress)
    }

    @Test(expected = UnknownHostException::class)
    fun dnsRefusesPlaceholderForIpTheUserNeverEntered() {
        LanCleartextBridge.dns.lookup("10-0-0-9.ollama-lan.invalid")
    }
}
