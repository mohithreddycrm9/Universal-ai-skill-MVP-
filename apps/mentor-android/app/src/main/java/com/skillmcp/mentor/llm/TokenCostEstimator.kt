package com.skillmcp.mentor.llm

object TokenCostEstimator {
    /** Rough token count (~4 chars per token). */
    fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    fun estimateUsd(
        profile: LlmProfile,
        draft: String,
        preset: ModelPreset,
        historyChars: Int = 0,
    ): Double {
        if (profile.kind == LlmProviderKind.OLLAMA) return 0.0
        val inputTokens = estimateTokens(draft) + historyChars / 4
        val outputTokens =
            when (preset) {
                ModelPreset.FAST -> 400
                ModelPreset.BALANCED -> 800
                ModelPreset.DEEP -> 2_000
            }
        val inputCost = inputTokens * profile.inputCostPer1M / 1_000_000.0
        val outputCost = outputTokens * profile.outputCostPer1M / 1_000_000.0
        return inputCost + outputCost
    }

    fun estimateReplyCost(profile: LlmProfile, replyText: String): Double {
        if (profile.kind == LlmProviderKind.OLLAMA) return 0.0
        val tokens = estimateTokens(replyText)
        return tokens * profile.outputCostPer1M / 1_000_000.0
    }
}
