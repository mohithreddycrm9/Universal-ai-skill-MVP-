package com.skillmcp.mentor.plugins

enum class PluginKind {
    /** Always available in chat — no toggle in Extensions. */
    BUILTIN,
    /** User enables in Extensions (e.g. network access). */
    OPTIONAL,
}

data class BuiltinPlugin(
    val id: String,
    val title: String,
    val description: String,
    val commands: List<String>,
    val kind: PluginKind = PluginKind.OPTIONAL,
)

object BuiltinPlugins {
    val builtIn: List<BuiltinPlugin> =
        listOf(
            BuiltinPlugin(
                id = "calc",
                title = "Calculator",
                description = "Evaluate math expressions.",
                commands = listOf("/calc 12 * (3 + 4)"),
                kind = PluginKind.BUILTIN,
            ),
            BuiltinPlugin(
                id = "time",
                title = "Date & time",
                description = "Current time in your locale or a named zone.",
                commands = listOf("/time", "/time Europe/London"),
                kind = PluginKind.BUILTIN,
            ),
            BuiltinPlugin(
                id = "units",
                title = "Unit convert",
                description = "Convert km/mi, kg/lb, °C/°F.",
                commands = listOf("/units 10 km mi", "/units 72 f c"),
                kind = PluginKind.BUILTIN,
            ),
            BuiltinPlugin(
                id = "uuid",
                title = "UUID",
                description = "Generate a random UUID.",
                commands = listOf("/uuid"),
                kind = PluginKind.BUILTIN,
            ),
            BuiltinPlugin(
                id = "wordcount",
                title = "Word count",
                description = "Count words in your message.",
                commands = listOf("/words your text here"),
                kind = PluginKind.BUILTIN,
            ),
        )

    val optional: List<BuiltinPlugin> =
        listOf(
            BuiltinPlugin(
                id = "fetch",
                title = "Fetch page text",
                description = "Pull plain text from a public HTTPS URL (max 24 KB).",
                commands = listOf("/fetch https://example.com"),
                kind = PluginKind.OPTIONAL,
            ),
        )

    val all: List<BuiltinPlugin> = builtIn + optional

    fun byId(id: String): BuiltinPlugin? = all.find { it.id == id }

    fun isAlwaysEnabled(id: String): Boolean = byId(id)?.kind == PluginKind.BUILTIN
}
