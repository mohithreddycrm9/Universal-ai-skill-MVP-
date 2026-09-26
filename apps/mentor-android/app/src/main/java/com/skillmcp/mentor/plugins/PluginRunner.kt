package com.skillmcp.mentor.plugins

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URL
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit
class PluginRunner(
    private val http: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build(),
) {
    fun runCommands(message: String, enabledPluginIds: Set<String>): String {
        val trimmed = message.trim()
        if (!trimmed.startsWith("/")) return ""
        val parts = trimmed.split(Regex("\\s+"), limit = 3)
        val command = parts[0].lowercase()
        val argLine = trimmed.removePrefix(parts[0]).trim()

        return when (command) {
            "/calc" -> if ("calc" in enabledPluginIds) section("Calculator", safeCalc(argLine)) else ""
            "/time" -> if ("time" in enabledPluginIds) section("Time", formatTime(argLine)) else ""
            "/units" -> if ("units" in enabledPluginIds) section("Units", convertUnits(argLine)) else ""
            "/fetch" -> if ("fetch" in enabledPluginIds) section("Fetched page", fetchUrl(argLine)) else ""
            "/uuid" -> if ("uuid" in enabledPluginIds) section("UUID", UUID.randomUUID().toString()) else ""
            "/words" -> if ("wordcount" in enabledPluginIds) section("Word count", countWords(argLine)) else ""
            else -> ""
        }
    }

    fun helpText(enabledPluginIds: Set<String>): String {
        val lines =
            BuiltinPlugins.all
                .filter { it.id in enabledPluginIds }
                .flatMap { plugin ->
                    listOf("- **${plugin.title}**: ${plugin.description}") +
                        plugin.commands.map { cmd -> "  - `$cmd`" }
                }
        if (lines.isEmpty()) return ""
        return "## On-device plugins\n" + lines.joinToString("\n")
    }

    private fun section(title: String, body: String): String = "## Plugin: $title\n$body"

    private fun safeCalc(expression: String): String {
        if (expression.isBlank()) return "Usage: /calc 2 + 2"
        val allowed = expression.filter { it.isDigit() || it in "+-*/().% " }
        if (allowed != expression) return "Only numbers and + - * / ( ) . % are allowed."
        return runCatching {
            val value = evalExpression(allowed.replace(" ", ""))
            "$expression = $value"
        }.getOrElse { "Could not evaluate: ${it.message}" }
    }

    private fun evalExpression(expr: String): Double {
        return ExpressionParser(expr).parse()
    }

    private fun formatTime(zoneArg: String): String {
        val zone = zoneArg.ifBlank { ZoneId.systemDefault().id }
        val zid = runCatching { ZoneId.of(zone) }.getOrElse { return "Unknown zone: $zone" }
        val now = ZonedDateTime.now(zid)
        val fmt = DateTimeFormatter.ofPattern("EEE, MMM d yyyy · HH:mm:ss z")
        return now.format(fmt)
    }

    private fun convertUnits(argLine: String): String {
        val tokens = argLine.split(Regex("\\s+"))
        if (tokens.size < 3) return "Usage: /units 10 km mi"
        val value = tokens[0].toDoubleOrNull() ?: return "Invalid number"
        val from = tokens[1].lowercase()
        val to = tokens[2].lowercase()
        val converted =
            when {
                from == "km" && to == "mi" -> value * 0.621371
                from == "mi" && to == "km" -> value / 0.621371
                from == "kg" && to == "lb" -> value * 2.20462
                from == "lb" && to == "kg" -> value / 2.20462
                from == "c" && to == "f" -> value * 9 / 5 + 32
                from == "f" && to == "c" -> (value - 32) * 5 / 9
                else -> return "Supported: km↔mi, kg↔lb, c↔f"
            }
        return "$value $from = ${"%.4f".format(converted)} $to"
    }

    private fun fetchUrl(urlText: String): String {
        val url = urlText.trim()
        if (!url.startsWith("https://")) return "Only https:// URLs are allowed."
        return runCatching {
            val host = URL(url).host.lowercase()
            if (com.skillmcp.mentor.util.PrivateNetworkGuards.isBlockedFetchHost(host)) return "Blocked host."
            http.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                val raw = response.body?.string() ?: ""
                if (!response.isSuccessful) return "HTTP ${response.code}"
                raw.replace(Regex("<[^>]+>"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .take(24_000)
            }
        }.getOrElse { "Fetch failed: ${it.message}" }
    }

    private fun countWords(text: String): String {
        val count = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        return "$count words"
    }

    /** Minimal recursive-descent parser for + - * / and parentheses. */
    private class ExpressionParser(private val input: String) {
        private var i = 0

        fun parse(): Double = parseExpr()

        private fun parseExpr(): Double {
            var value = parseTerm()
            while (i < input.length) {
                when (input[i]) {
                    '+' -> {
                        i++
                        value += parseTerm()
                    }
                    '-' -> {
                        i++
                        value -= parseTerm()
                    }
                    else -> break
                }
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (i < input.length) {
                when (input[i]) {
                    '*' -> {
                        i++
                        value *= parseFactor()
                    }
                    '/' -> {
                        i++
                        value /= parseFactor()
                    }
                    '%' -> {
                        i++
                        value %= parseFactor()
                    }
                    else -> break
                }
            }
            return value
        }

        private fun parseFactor(): Double {
            if (i < input.length && input[i] == '-') {
                i++
                return -parseFactor()
            }
            if (i < input.length && input[i] == '(') {
                i++
                val inner = parseExpr()
                if (i < input.length && input[i] == ')') i++
                return inner
            }
            val start = i
            while (i < input.length && (input[i].isDigit() || input[i] == '.')) i++
            return input.substring(start, i).toDouble()
        }
    }
}
