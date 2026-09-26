package com.skillmcp.mentor.plugins

data class BuiltinPlugin(
    val id: String,
    val title: String,
    val description: String,
    val commands: List<String>,
)

object BuiltinPlugins {
    val all: List<BuiltinPlugin> =
        listOf(
            BuiltinPlugin(
                id = "calc",
                title = "Calculator",
                description = "Evaluate math expressions safely.",
                commands = listOf("/calc 12 * (3 + 4)"),
            ),
            BuiltinPlugin(
                id = "time",
                title = "Date & time",
                description = "Current time in your locale or a named zone.",
                commands = listOf("/time", "/time Europe/London"),
            ),
            BuiltinPlugin(
                id = "units",
                title = "Unit convert",
                description = "Convert common units (km/mi, kg/lb, C/F).",
                commands = listOf("/units 10 km mi", "/units 72 f c"),
            ),
            BuiltinPlugin(
                id = "fetch",
                title = "Fetch page text",
                description = "Pull plain text from a public HTTPS URL (max 24 KB).",
                commands = listOf("/fetch https://example.com"),
            ),
            BuiltinPlugin(
                id = "uuid",
                title = "UUID",
                description = "Generate a random UUID v4.",
                commands = listOf("/uuid"),
            ),
            BuiltinPlugin(
                id = "wordcount",
                title = "Word count",
                description = "Count words in the rest of your message.",
                commands = listOf("/words your text here"),
            ),
        )

    fun byId(id: String): BuiltinPlugin? = all.find { it.id == id }
}
