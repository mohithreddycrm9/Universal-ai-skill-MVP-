package com.skillmcp.mentor.llm

import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import java.net.InetAddress
import javax.net.ssl.HttpsURLConnection
import java.util.concurrent.TimeUnit

/** HTTPS for cloud APIs; cleartext only when the request host is a private/LAN address (Ollama). */
object OllamaHttpClient {
    fun create(
        connectSec: Long = 30,
        readSec: Long = 180,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(connectSec, TimeUnit.SECONDS)
            .readTimeout(readSec, TimeUnit.SECONDS)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
            .hostnameVerifier { hostname, session ->
                if (isPrivateOrLocalHost(hostname)) {
                    true
                } else {
                    HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
                }
            }
            .build()

    fun isPrivateOrLocalHost(host: String): Boolean {
        if (host.equals("localhost", ignoreCase = true)) return true
        return runCatching {
            val addr = InetAddress.getByName(host)
            addr.isLoopbackAddress || addr.isLinkLocalAddress || addr.isSiteLocalAddress
        }.getOrDefault(false)
    }
}
