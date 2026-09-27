package com.skillmcp.mentor.util

import java.net.InetAddress
import java.net.URI

object PrivateNetworkGuards {
    fun isBlockedFetchHost(host: String): Boolean {
        val h = host.lowercase().trim()
        if (h == "localhost" || h.endsWith(".local")) return true
        return runCatching {
            val address = InetAddress.getByName(h)
            address.isLoopbackAddress ||
                address.isLinkLocalAddress ||
                address.isSiteLocalAddress ||
                address.isAnyLocalAddress
        }.getOrDefault(false)
    }

    fun isCleartextAllowedHost(host: String): Boolean {
        val h = host.lowercase()
        if (h == "localhost" || h == "10.0.2.2" || h == "127.0.0.1") return true
        return runCatching {
            val address = InetAddress.getByName(h)
            address.isLoopbackAddress || address.isSiteLocalAddress
        }.getOrDefault(false)
    }

    fun hostFromUrl(url: String): String? = runCatching { URI(url).host }.getOrNull()
}
