package com.skillmcp.mentor.llm

object HuggingFaceDefaults {
    const val ROUTER_BASE_URL = "https://router.huggingface.co/v1/"

    /** Strip `:fastest`, `:provider`, etc. for serverless model paths. */
    fun serverlessModelId(model: String): String = model.trim().substringBefore(':').trim()

    val featuredChatModels: List<Pair<String, String>> =
        listOf(
            "meta-llama/Meta-Llama-3-8B-Instruct:fastest" to "Llama 3 8B Instruct",
            "Qwen/Qwen2.5-7B-Instruct:fastest" to "Qwen 2.5 7B Instruct",
            "google/gemma-2-2b-it:fastest" to "Gemma 2 2B IT",
            "mistralai/Mistral-7B-Instruct-v0.3:fastest" to "Mistral 7B Instruct",
        )
}
