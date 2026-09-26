package com.skillmcp.mentor.llm

import com.skillmcp.mentor.data.LlmSecureStore
import com.skillmcp.mentor.data.UserPreferences
import com.skillmcp.mentor.data.db.LlmProfileEntity
import com.skillmcp.mentor.data.db.LlmUsageEntity
import com.skillmcp.mentor.data.db.MentorDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LlmProfileRepository(
    private val dao: MentorDao,
    private val secureStore: LlmSecureStore,
    private val userPreferences: UserPreferences,
) {
    fun observeProfiles(): Flow<List<LlmProfile>> =
        dao.observeLlmProfiles().map { rows -> rows.map { it.toProfile(secureStore.getKey(it.id)) } }

    suspend fun ensureDefaults() {
        if (dao.allLlmProfiles().isEmpty()) {
            defaultLlmProfiles().forEach { profile ->
                dao.upsertLlmProfile(profile.toEntity())
                if (profile.apiKey.isNotBlank()) {
                    secureStore.setKey(profile.id, profile.apiKey)
                }
            }
        }
        migrateLegacyGoogleProfileId()
        val prefs = userPreferences.current()
        if (prefs.activeLlmProfileId.isBlank()) {
            userPreferences.update { it.copy(activeLlmProfileId = "openai") }
        }
        migrateLegacyPrefsIfNeeded()
    }

    private suspend fun migrateLegacyGoogleProfileId() {
        val legacy = dao.getLlmProfile("gemini")
        if (legacy != null) {
            val apiKey = secureStore.getKey("gemini")
            dao.deleteLlmProfile("gemini")
            secureStore.deleteKey("gemini")
            dao.upsertLlmProfile(
                legacy.copy(
                    id = "google-gen",
                    name = "Google AI",
                ),
            )
            if (apiKey.isNotBlank()) {
                secureStore.setKey("google-gen", apiKey)
            }
        }
        if (userPreferences.current().activeLlmProfileId == "gemini") {
            userPreferences.update { it.copy(activeLlmProfileId = "google-gen") }
        }
    }

    private suspend fun migrateLegacyPrefsIfNeeded() {
        val prefs = userPreferences.current()
        if (prefs.llmApiKey.isBlank() && prefs.llmBaseUrl == "https://api.openai.com/v1/") return
        val customId = "custom-legacy"
        if (dao.getLlmProfile(customId) == null) {
            dao.upsertLlmProfile(
                LlmProfileEntity(
                    id = customId,
                    name = "Custom (migrated)",
                    kind = LlmProviderKind.OPENAI_COMPAT.name,
                    baseUrl = prefs.llmBaseUrl,
                    model = prefs.llmModel,
                    inputCostPer1M = 0.0,
                    outputCostPer1M = 0.0,
                    isBuiltIn = false,
                ),
            )
            if (prefs.llmApiKey.isNotBlank()) {
                secureStore.setKey(customId, prefs.llmApiKey)
            }
            userPreferences.update {
                it.copy(
                    activeLlmProfileId = customId,
                    llmApiKey = "",
                )
            }
        }
    }

    suspend fun activeProfile(): LlmProfile {
        val id = userPreferences.current().activeLlmProfileId.ifBlank { "openai" }
        val entity = dao.getLlmProfile(id) ?: dao.allLlmProfiles().firstOrNull()
        return entity?.toProfile(secureStore.getKey(entity.id))
            ?: defaultLlmProfiles().first().copy(apiKey = secureStore.getKey("openai"))
    }

    suspend fun setActiveProfile(id: String) {
        userPreferences.update { it.copy(activeLlmProfileId = id) }
    }

    suspend fun upsertProfile(profile: LlmProfile) {
        dao.upsertLlmProfile(profile.toEntity())
        secureStore.setKey(profile.id, profile.apiKey)
    }

    suspend fun deleteProfile(id: String) {
        dao.deleteLlmProfile(id)
        secureStore.deleteKey(id)
        val active = userPreferences.current().activeLlmProfileId
        if (active == id) {
            userPreferences.update { it.copy(activeLlmProfileId = "openai") }
        }
    }

    suspend fun recordUsage(
        profile: LlmProfile,
        result: LlmChatResult,
        success: Boolean,
        errorMessage: String? = null,
    ) {
        val usage = result.usage
        val cost =
            estimateCostUsd(usage, profile.inputCostPer1M, profile.outputCostPer1M)
        dao.insertLlmUsage(
            LlmUsageEntity(
                id = UUID.randomUUID().toString(),
                profileId = profile.id,
                providerName = profile.name,
                model = result.model,
                promptTokens = usage?.promptTokens ?: 0,
                completionTokens = usage?.completionTokens ?: 0,
                totalTokens = usage?.totalTokens ?: 0,
                estimatedUsd = cost,
                latencyMs = result.latencyMs,
                success = success,
                errorMessage = errorMessage,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    fun observeUsageTotals(sinceMs: Long): Flow<UsageTotals> =
        dao.observeUsageTotals(sinceMs).map { row ->
            UsageTotals(
                requestCount = row.requestCount,
                promptTokens = row.promptTokens,
                completionTokens = row.completionTokens,
                estimatedUsd = row.estimatedUsd,
            )
        }

    fun observeUsageByModel(sinceMs: Long): Flow<List<UsageByModelRow>> =
        dao.observeUsageByModel(sinceMs).map { rows ->
            rows.map {
                UsageByModelRow(
                    providerName = it.providerName,
                    model = it.model,
                    requestCount = it.requestCount,
                    totalTokens = it.totalTokens,
                    estimatedUsd = it.estimatedUsd,
                )
            }
        }

    fun observeUsageByDay(sinceMs: Long): Flow<List<UsageByDayRow>> =
        dao.observeUsageByDay(sinceMs).map { rows ->
            rows.map {
                UsageByDayRow(
                    dayKey = it.dayKey,
                    requestCount = it.requestCount,
                    totalTokens = it.totalTokens,
                    estimatedUsd = it.estimatedUsd,
                )
            }
        }
}

private fun LlmProfileEntity.toProfile(apiKey: String): LlmProfile =
    LlmProfile(
        id = id,
        name = name,
        kind = LlmProviderKind.entries.find { it.name == kind } ?: LlmProviderKind.OPENAI_COMPAT,
        baseUrl = baseUrl,
        model = model,
        apiKey = apiKey,
        inputCostPer1M = inputCostPer1M,
        outputCostPer1M = outputCostPer1M,
        isBuiltIn = isBuiltIn,
    )

private fun LlmProfile.toEntity(): LlmProfileEntity =
    LlmProfileEntity(
        id = id,
        name = name,
        kind = kind.name,
        baseUrl = baseUrl,
        model = model,
        inputCostPer1M = inputCostPer1M,
        outputCostPer1M = outputCostPer1M,
        isBuiltIn = isBuiltIn,
    )
