package com.skillmcp.mentor.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object UserFacingErrors {
    fun message(throwable: Throwable): String {
        val raw = throwable.message?.trim().orEmpty()
        when {
            throwable is UnknownHostException ->
                return "No network connection. Check Wi‑Fi or mobile data and try again."
            throwable is SocketTimeoutException ->
                return "The request timed out. Try again or pick a smaller model."
            raw.contains("Backup URL not configured", ignoreCase = true) ->
                return "Backup is not configured."
            raw.contains("Spend limit", ignoreCase = true) || raw.contains("budget", ignoreCase = true) ->
                return raw.take(200)
            raw.contains("API key", ignoreCase = true) || raw.contains("401", ignoreCase = true) ->
                return "Authentication failed. Open Models and update your API key or sign-in."
            raw.contains("403", ignoreCase = true) ->
                return "Access denied by the provider. Check your key permissions."
            raw.contains("429", ignoreCase = true) ->
                return "Rate limited by the provider. Wait a moment and try again."
            raw.contains("HTTP 5", ignoreCase = true) ->
                return "The model provider returned a server error. Try again later."
            raw.contains("HTTP 4", ignoreCase = true) ->
                return "The model provider rejected the request. Check model id and account."
            raw.length > 180 -> return "Something went wrong while contacting the model. Check Models and try again."
            raw.isNotEmpty() -> return raw
            else -> return "Something went wrong. Please try again."
        }
    }

    fun redactForStorage(throwable: Throwable): String =
        message(throwable).take(500)
}
