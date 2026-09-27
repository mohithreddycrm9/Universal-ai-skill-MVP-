package com.skillmcp.mentor.skills.finder

/**
 * Topic buckets for the finder's category chips. Derived from each skill's own name first (the
 * most specific signal), then its description; nothing is assigned by hand.
 */
enum class SkillCategory(private val keywords: List<String>) {
    DOCUMENTS(listOf("pdf", "docx", "xlsx", "pptx", "spreadsheet", "slides", "document", "docs", "notebook")),
    SECURITY(listOf("security", "secops", "threat", "vulnerab", "detection", "sentinel", "defender")),
    DESIGN(listOf("design", "figma", "brand", "canvas", "art", "css", "visual", "theme", "animation", "gif", "ui")),
    WRITING(listOf("writing", "blog", "comms", "copywriting", "newsletter", "coauthoring")),
    DATA_AI(listOf("dataset", "hugging", "llm", "training", "postgres", "sql", "database", "analytics", "bigquery", "data", "kql", "speech", "transcribe")),
    CLOUD(listOf("azure", "cloud", "aws", "gcloud", "kubernetes", "gke", "deploy", "hosting", "serverless", "vercel", "eas", "netlify", "render")),
    CODING(listOf("code", "sdk", "api", "react", "test", "testing", "debug", "cli", "python", "typescript", "java", "review", "git", "gh", "mobile", "expo", "playwright", "ci", "mcp")),
    GENERAL(emptyList()),
    ;

    private fun matches(words: List<String>): Boolean =
        keywords.any { k -> words.any { w -> w == k || (k.length >= 5 && w.startsWith(k)) } }

    companion object {
        private val wordSplit = Regex("[^a-z0-9+#]+")

        private fun words(text: String) = text.lowercase().split(wordSplit).filter { it.isNotBlank() }

        fun classify(name: String, description: String): SkillCategory {
            val nameWords = words(name)
            val descriptionWords = words(description)
            return entries.firstOrNull { it != GENERAL && it.matches(nameWords) }
                ?: entries.firstOrNull { it != GENERAL && it.matches(descriptionWords) }
                ?: GENERAL
        }
    }
}
