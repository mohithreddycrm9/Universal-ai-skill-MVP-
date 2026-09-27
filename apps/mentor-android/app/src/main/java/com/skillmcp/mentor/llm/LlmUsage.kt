package com.skillmcp.mentor.llm

data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)

data class LlmChatResult(
    val content: String,
    val usage: TokenUsage?,
    val model: String,
    val latencyMs: Long,
)

data class UsageTotals(
    val requestCount: Int,
    val promptTokens: Long,
    val completionTokens: Long,
    val estimatedUsd: Double,
)

data class UsageByModelRow(
    val providerName: String,
    val model: String,
    val requestCount: Int,
    val totalTokens: Long,
    val estimatedUsd: Double,
)

data class UsageByDayRow(
    val dayKey: String,
    val requestCount: Int,
    val totalTokens: Long,
    val estimatedUsd: Double,
)

