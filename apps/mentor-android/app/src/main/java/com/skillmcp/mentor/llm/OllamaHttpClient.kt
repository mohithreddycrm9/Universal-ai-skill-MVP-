package com.skillmcp.mentor.llm

import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

/** HTTPS for cloud APIs; cleartext only for literal private/LAN hosts the user configures (Ollama). */
object OllamaHttpClient {
    fun create(
        connectSec: Long = 30,
        readSec: Long = 180,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(connectSec, TimeUnit.SECONDS)
            .readTimeout(readSec, TimeUnit.SECONDS)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
            // Home-network Ollama over http:// via the NSC-permitted placeholder zone (see LanCleartextBridge).
            .addInterceptor(LanCleartextBridge.interceptor)
            .dns(LanCleartextBridge.dns)
            // A redirect would skip the bridge; never follow one to another host in cleartext.
            .followRedirects(false)
            .hostnameVerifier { hostname, session ->
                if (isLiteralPrivateOrLocalHost(hostname)) {
                    true
                } else {
                    HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
                }
            }
            .build()

    /**
     * Only trust cleartext for hosts that are already IP literals or localhost —
     * do not resolve DNS (avoids rebinding public hostnames to private IPs).
     */
    fun isLiteralPrivateOrLocalHost(host: String): Boolean {
        val h = host.trim()
        if (h.equals("localhost", ignoreCase = true)) return true
        if (h == "127.0.0.1" || h == "10.0.2.2" || h == "::1") return true
        return runCatching {
            val addr = InetAddress.getByName(h)
            if (h.none { it.isLetter() }) {
                addr.isLoopbackAddress || addr.isLinkLocalAddress || addr.isSiteLocalAddress
            } else {
                false
            }
        }.getOrDefault(false)
    }
}
