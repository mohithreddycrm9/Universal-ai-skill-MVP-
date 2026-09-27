package com.skillmcp.mentor.skills.finder

/** A request to build/make something, reduced to searchable topic words and an optional company. */
data class BuildIntent(
    val terms: List<String>,
    /** Company named in the request (from [OfficialOrgAllowlist]), e.g. "Supabase" or "ServiceNow". */
    val company: String?,
)

/**
 * On-device build-intent detection (no model call). English plus common Hindi, Telugu and Tamil
 * phrasings, including romanised Hindi ("banao").
 */
object BuildIntentDetector {
    private val englishVerbs =
        setOf(
            "build", "building", "make", "making", "create", "creating", "generate", "generating",
            "develop", "developing", "scaffold", "deploy", "deploying", "setup", "implement", "design",
            "draft", "prepare", "code", "ship", "automate", "write", "produce", "convert", "migrate",
        )
    private val phraseVerbs = listOf("set up", "put together", "spin up")
    private val indicStems =
        listOf(
            // Hindi
            "बनाना", "बनाओ", "बनाएं", "बनाएँ", "बना दो", "बनाने", "बनानी", "बनाइए", "तैयार कर", "बना",
            // Telugu
            "తయారు", "నిర్మించ", "సృష్టించ", "రూపొందించ", "చేయాలి", "బిల్డ్",
            // Tamil
            "உருவாக்க", "தயார் செய்", "தயாரிக்க", "கட்டமை", "பில்ட்",
        )
    private val romanHindi = setOf("banao", "banana", "banado", "banani", "bana")

    private val stopWords =
        setOf(
            "a", "an", "the", "i", "me", "my", "we", "our", "you", "your", "to", "for", "of", "in", "on", "with",
            "and", "or", "please", "can", "could", "would", "help", "want", "need", "how", "do", "does", "is",
            "it", "that", "this", "some", "new", "simple", "quick", "using", "use", "from", "into", "app", "hai",
            "karo", "mujhe", "ek", "chahiye", "let's", "lets", "like", "about", "what", "should", "will", "just",
        ) + englishVerbs + romanHindi - setOf("deploy", "deploying", "design", "migrate", "convert", "automate")

    /** Maps everyday words to the vocabulary skills use in their names and descriptions. */
    private val synonyms: Map<String, List<String>> =
        mapOf(
            "next.js" to listOf("next.js", "nextjs", "react", "vercel"),
            "nextjs" to listOf("next.js", "nextjs", "react", "vercel"),
            "slide" to listOf("pptx", "presentation"),
            "slides" to listOf("pptx", "presentation"),
            "deck" to listOf("pptx", "presentation"),
            "presentation" to listOf("pptx", "presentation"),
            "powerpoint" to listOf("pptx"),
            "spreadsheet" to listOf("xlsx", "spreadsheet"),
            "excel" to listOf("xlsx", "spreadsheet"),
            "word" to listOf("docx"),
            "website" to listOf("frontend", "web"),
            "webpage" to listOf("frontend", "web"),
            "landing" to listOf("frontend", "web"),
            "android" to listOf("expo", "mobile", "native"),
            "ios" to listOf("expo", "mobile", "native"),
            "mobile" to listOf("expo", "mobile"),
            "postgres" to listOf("postgres", "supabase"),
            "database" to listOf("postgres", "database"),
            "पीडीएफ" to listOf("pdf"),
            "प्रेजेंटेशन" to listOf("pptx", "presentation"),
            "వెబ్‌సైట్" to listOf("frontend", "web"),
            "இணையதளம்" to listOf("frontend", "web"),
        )

    private val splitter = Regex("[^\\p{L}\\p{M}\\p{N}.+#]+")

    fun detect(text: String): BuildIntent? {
        val lower = text.lowercase().trim()
        if (lower.isEmpty()) return null
        val words = lower.split(splitter).map { it.trim('.') }.filter { it.isNotBlank() }
        val hasVerb =
            words.any { it in englishVerbs || it in romanHindi } ||
                phraseVerbs.any { it in lower } ||
                indicStems.any { it in lower }
        if (!hasVerb) return null
        val company = companyIn(lower, words)
        val companyWords =
            company?.let { c ->
                OfficialOrgAllowlist.orgsOf(c).flatMap { listOf(it.company.lowercase(), it.login.lowercase()) + it.aliases }.flatMap { it.split(' ') }.toSet()
            }.orEmpty()
        val terms =
            words
                .filter { it !in stopWords && it !in companyWords && it.length >= 2 && indicStems.none { stem -> it.startsWith(stem) } }
                .flatMap { synonyms[it] ?: listOf(it) }
                .distinct()
        if (terms.isEmpty() && company == null) return null
        return BuildIntent(terms, company)
    }

    private fun companyIn(lower: String, words: List<String>): String? =
        OfficialOrgAllowlist.orgs.firstOrNull { org ->
            org.company.lowercase() in words ||
                org.login.lowercase() in words ||
                (org.company.contains(' ') && org.company.lowercase() in lower) ||
                org.aliases.any { alias -> if (' ' in alias) alias in lower else alias in words }
        }?.company
}

/** Ranks verified skills for a [BuildIntent]. Pure; the input list is already policy-checked. */
object OfficialSkillMatcher {
    private val wordSplit = Regex("[^a-z0-9.+#]+")

    fun match(
        intent: BuildIntent,
        skills: List<OfficialSkill>,
        max: Int = 3,
    ): List<OfficialSkill> {
        val pool = if (intent.company != null) skills.filter { it.company.equals(intent.company, ignoreCase = true) } else skills
        if (intent.terms.isEmpty()) return companyDefaults(intent, pool, max)
        val ranked = rank(intent, pool, max)
        return ranked.ifEmpty { companyDefaults(intent, pool, max) }
    }

    /** "Build on Supabase" with no clearer topic: that company's own general skill(s), name first. */
    private fun companyDefaults(
        intent: BuildIntent,
        pool: List<OfficialSkill>,
        max: Int,
    ): List<OfficialSkill> {
        val company = intent.company?.lowercase() ?: return emptyList()
        val key = company.replace(" ", "")
        return pool.filter { key in it.name.lowercase().replace("-", "") }.sortedBy { it.name.length }.take(max)
    }

    private fun rank(
        intent: BuildIntent,
        pool: List<OfficialSkill>,
        max: Int,
    ): List<OfficialSkill> {
        return pool
            .mapNotNull { skill ->
                val nameWords = skill.name.lowercase().split(wordSplit, 0).flatMap { it.split('-') }.filter { it.isNotBlank() }.toSet()
                val descWords = skill.description.lowercase().split(wordSplit).map { it.trim('.') }.filter { it.isNotBlank() }.toSet()
                var score = 0
                var nameHits = 0
                for (t in intent.terms) {
                    when {
                        t in nameWords -> {
                            score += 6
                            nameHits++
                        }
                        t.length >= 4 && nameWords.any { it.startsWith(t) } -> {
                            score += 4
                            nameHits++
                        }
                        t in descWords -> score += 2
                    }
                }
                if (score < 4) null else Triple(score, nameHits, skill)
            }
            .sortedWith(
                compareByDescending<Triple<Int, Int, OfficialSkill>> { it.first }
                    .thenByDescending { it.second }
                    .thenBy { it.third.name.length }
                    .thenBy { it.third.company }
                    .thenBy { it.third.name },
            )
            .map { it.third }
            .distinctBy { it.name.lowercase() }
            .take(max)
    }
}
