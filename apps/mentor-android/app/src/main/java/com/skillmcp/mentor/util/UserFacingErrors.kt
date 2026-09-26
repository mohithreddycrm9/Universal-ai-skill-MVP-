package com.skillmcp.mentor.util

import com.skillmcp.mentor.policy.SpendLimitException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class UserFacingError(
    val summary: String,
    val details: String? = null,
)

object UserFacingErrors {
    fun parse(throwable: Throwable): UserFacingError {
        val raw = throwable.message?.trim().orEmpty()
        return when {
            throwable is UnknownHostException ->
                UserFacingError("No network connection. Check Wi‑Fi or mobile data and try again.")
            throwable is SocketTimeoutException ->
                UserFacingError("The request timed out. Try again or pick a smaller model.")
            raw.contains("Backup URL not configured", ignoreCase = true) ->
                UserFacingError("Backup is not configured.")
            throwable is SpendLimitException ->
                UserFacingError(
                    throwable.check.message ?: "Estimated spend limit reached.",
                    details = raw.takeIf { it.isNotBlank() },
                )
            raw.contains("Spend limit", ignoreCase = true) ||
                raw.contains("estimated spend", ignoreCase = true) ||
                raw.contains("budget", ignoreCase = true) ->
                UserFacingError(raw.take(280), details = raw.takeIf { it.length > 280 })
            raw.contains("API key", ignoreCase = true) || raw.contains("401", ignoreCase = true) ->
                UserFacingError(
                    "Authentication failed. Open Models and update your API key or sign-in.",
                    details = raw.takeIf { it.isNotBlank() },
                )
            raw.contains("403", ignoreCase = true) ->
                UserFacingError("Access denied by the provider. Check your key permissions.", details = raw)
            raw.contains("429", ignoreCase = true) ->
                UserFacingError("Rate limited by the provider. Wait a moment and try again.", details = raw)
            raw.contains("HTTP 5", ignoreCase = true) ->
                UserFacingError(
                    "The model provider returned a server error. Try again later.",
                    details = raw,
                )
            raw.contains("HTTP 4", ignoreCase = true) ->
                UserFacingError(
                    "The model provider rejected the request. Check model id and account.",
                    details = raw,
                )
            raw.length > 180 ->
                UserFacingError(
                    "Something went wrong while contacting the model. Check Models and try again.",
                    details = raw,
                )
            raw.isNotEmpty() -> UserFacingError(raw)
            else -> UserFacingError("Something went wrong. Please try again.")
        }
    }

    fun message(throwable: Throwable): String = parse(throwable).summary

    fun redactForStorage(throwable: Throwable): String =
        message(throwable).take(500)
}
