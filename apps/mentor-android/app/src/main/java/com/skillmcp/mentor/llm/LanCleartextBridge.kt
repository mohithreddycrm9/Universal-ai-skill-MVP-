package com.skillmcp.mentor.llm

import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * Lets the Ollama client reach `http://<private IPv4>:port` on the user's home network while the
 * app-wide Network Security Config stays cleartext-off.
 *
 * Android's NSC cannot list IP ranges, and OkHttp refuses cleartext to any host NSC does not permit.
 * NSC permits cleartext only for the reserved placeholder zone [ZONE] (`.invalid` never resolves
 * publicly). The Ollama client's [Interceptor] rewrites a user-entered private IP such as
 * `192.168.1.42` to `192-168-1-42.ollama-lan.invalid`, keeps `Host: 192.168.1.42:11434`, and
 * registers the IP; its [Dns] maps the placeholder back **only** for registered private IPs.
 * Every other client uses the system DNS, so the placeholder is unreachable from them and public
 * `http://` stays blocked everywhere.
 */
object LanCleartextBridge {
    const val ZONE = "ollama-lan.invalid"

    /** Hosts NSC already allows in cleartext (emulator/loopback); no bridge needed. */
    private val nscCleartextHosts = setOf("localhost", "127.0.0.1", "10.0.2.2")

    private val registered = ConcurrentHashMap.newKeySet<String>()

    /** RFC 1918 IPv4 literal (10/8, 172.16/12, 192.168/16). No DNS lookups. */
    fun isPrivateIpv4Literal(host: String): Boolean {
        val octets = parseIpv4(host) ?: return false
        val a = octets[0]
        val b = octets[1]
        return a == 10 || (a == 172 && b in 16..31) || (a == 192 && b == 168)
    }

    fun encodeHost(ip: String): String = ip.replace('.', '-') + "." + ZONE

    /** Returns the private IPv4 for a placeholder host, or null if it is not a valid bridge host. */
    fun decodeHost(host: String): String? {
        val suffix = ".$ZONE"
        if (!host.endsWith(suffix, ignoreCase = true)) return null
        val ip = host.dropLast(suffix.length).replace('-', '.')
        return ip.takeIf { isPrivateIpv4Literal(it) }
    }

    fun needsBridge(url: HttpUrl): Boolean =
        !url.isHttps && url.host !in nscCleartextHosts && isPrivateIpv4Literal(url.host)

    /** Registers the user-entered IP and returns the request pointed at its placeholder host. */
    fun bridge(request: Request): Request {
        val url = request.url
        if (!needsBridge(url)) return request
        val ip = url.host
        registered += ip
        return request.newBuilder()
            .url(url.newBuilder().host(encodeHost(ip)).build())
            .header("Host", "$ip:${url.port}")
            .build()
    }

    fun isRegistered(ip: String): Boolean = ip in registered

    internal fun clearForTest() = registered.clear()

    private fun parseIpv4(host: String): IntArray? {
        val parts = host.split('.')
        if (parts.size != 4) return null
        val out = IntArray(4)
        parts.forEachIndexed { i, p ->
            if (p.isEmpty() || p.length > 3 || p.any { !it.isDigit() }) return null
            if (p.length > 1 && p[0] == '0') return null
            val v = p.toInt()
            if (v > 255) return null
            out[i] = v
        }
        return out
    }

    val interceptor: Interceptor =
        Interceptor { chain: Interceptor.Chain -> chain.proceed(bridge(chain.request())) }

    /** Resolves placeholder hosts only for registered private IPs; everything else goes to system DNS. */
    val dns: Dns =
        object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val ip = decodeHost(hostname)
                return when {
                    ip != null && isRegistered(ip) ->
                        listOf(
                            InetAddress.getByAddress(
                                hostname,
                                ip.split('.').map { it.toInt().toByte() }.toByteArray(),
                            ),
                        )
                    ip != null -> throw java.net.UnknownHostException("Ollama host $ip was not configured in this app")
                    else -> Dns.SYSTEM.lookup(hostname)
                }
            }
        }
}
