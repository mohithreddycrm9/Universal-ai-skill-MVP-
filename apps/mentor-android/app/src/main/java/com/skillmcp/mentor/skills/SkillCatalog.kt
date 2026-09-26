package com.skillmcp.mentor.skills

data class CatalogSkill(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val sourceUrl: String,
    val trustTier: String = "Curated",
)

object SkillCatalog {
    val featured: List<CatalogSkill> =
        listOf(
            CatalogSkill(
                id = "universal-skill-trust",
                title = "Skill trust & safety",
                description = "Discover, verify, and use agent skills with a security-first workflow.",
                category = "Agent tooling",
                sourceUrl = "https://github.com/mohithreddycrm9/Universal-ai-skill-MVP-",
                trustTier = "Official",
            ),
            CatalogSkill(
                id = "anthropic-skills",
                title = "Anthropic skill examples",
                description = "Reference skill packs for writing, analysis, and workflows.",
                category = "Productivity",
                sourceUrl = "https://github.com/anthropics/skills",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "openai-cookbook",
                title = "OpenAI cookbook patterns",
                description = "Prompting and API patterns for assistants and tools.",
                category = "Coding",
                sourceUrl = "https://github.com/openai/openai-cookbook",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "cursor-skills",
                title = "Cursor agent skills",
                description = "Patterns for IDE agents, hooks, and project context.",
                category = "Coding",
                sourceUrl = "https://github.com/getcursor/cursor",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "langchain-templates",
                title = "LangChain templates",
                description = "Chains and agents for retrieval, tools, and chat.",
                category = "Agent tooling",
                sourceUrl = "https://github.com/langchain-ai/langchain",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "microsoft-ai-skills",
                title = "Microsoft AI samples",
                description = "Samples for copilots, RAG, and responsible AI checks.",
                category = "Enterprise",
                sourceUrl = "https://github.com/microsoft/ai-agents-for-beginners",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "sqlite-skill",
                title = "SQLite assistant",
                description = "Schema-aware SQL help and safe query guidance.",
                category = "Data",
                sourceUrl = "https://github.com/mohithreddycrm9/Universal-ai-skill-MVP-/tree/main/examples/skills/benign/csv-normalize",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "markdown-outline",
                title = "Markdown outliner",
                description = "Structure long documents and outlines with consistent headings.",
                category = "Writing",
                sourceUrl = "https://github.com/mohithreddycrm9/Universal-ai-skill-MVP-/tree/main/examples/skills/benign/markdown-outline",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "mcp-docs",
                title = "MCP integration guide",
                description = "Model Context Protocol patterns for tools and resources.",
                category = "Agent tooling",
                sourceUrl = "https://github.com/modelcontextprotocol/servers",
                trustTier = "Curated",
            ),
            CatalogSkill(
                id = "huggingface-skills",
                title = "Hugging Face agents",
                description = "ML workflows, datasets, and inference patterns.",
                category = "ML",
                sourceUrl = "https://github.com/huggingface/agents-course",
                trustTier = "Curated",
            ),
        )
}
