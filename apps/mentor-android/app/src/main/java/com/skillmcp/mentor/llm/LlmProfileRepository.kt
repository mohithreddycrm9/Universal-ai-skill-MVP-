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
        dao.observeLlmProfiles().map { rows ->
            rows.map {
                it.toProfile(
                    apiKey = secureStore.getKey(it.id),
                    linkedAccount = secureStore.getLinkedAccount(it.id),
                )
            }
        }

    suspend fun ensureDefaults() {
        if (dao.allLlmProfiles().isEmpty()) {
            defaultLlmProfiles().forEach { profile ->
                dao.upsertLlmProfile(profile.toEntity())
                if (profile.apiKey.isNotBlank()) {
                    secureStore.setKey(profile.id, profile.apiKey)
                }
            }
        }
        ensureBuiltInProfile("huggingface", defaultLlmProfiles().find { it.id == "huggingface" })
        migrateLegacyGoogleProfileId()
        val prefs = userPreferences.current()
        if (prefs.activeLlmProfileId.isBlank()) {
            userPreferences.update { it.copy(activeLlmProfileId = "openai") }
        }
        migrateLegacyPrefsIfNeeded()
    }

    private suspend fun ensureBuiltInProfile(id: String, profile: LlmProfile?) {
        if (profile == null || dao.getLlmProfile(id) != null) return
        dao.upsertLlmProfile(profile.toEntity())
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

    suspend fun testProfile(profile: LlmProfile): Result<String> {
        val client = MultiLlmClient()
        return client.chat(
            profile = profile,
            systemPrompt = "You are a connection test.",
            history = emptyList(),
            userMessage = "Reply with exactly: OK",
        ).map { it.content.take(80) }
    }

    suspend fun activeProfile(): LlmProfile {
        val id = userPreferences.current().activeLlmProfileId.ifBlank { "openai" }
        val entity = dao.getLlmProfile(id) ?: dao.allLlmProfiles().firstOrNull()
        return entity?.toProfile(
            apiKey = secureStore.getKey(entity.id),
            linkedAccount = secureStore.getLinkedAccount(entity.id),
        ) ?: defaultLlmProfiles().first().copy(
            apiKey = secureStore.getKey("openai"),
            linkedAccount = secureStore.getLinkedAccount("openai"),
        )
    }

    /** A saved profile by id (with its key), or null if it was deleted. */
    suspend fun profileById(id: String): LlmProfile? =
        dao.getLlmProfile(id)?.let {
            it.toProfile(apiKey = secureStore.getKey(it.id), linkedAccount = secureStore.getLinkedAccount(it.id))
        }

    suspend fun setActiveProfile(id: String) {
        userPreferences.update { it.copy(activeLlmProfileId = id) }
    }

    suspend fun upsertProfile(profile: LlmProfile) {
        dao.upsertLlmProfile(profile.toEntity())
        secureStore.setKey(profile.id, profile.apiKey)
        secureStore.setLinkedAccount(profile.id, profile.linkedAccount)
    }

    suspend fun disconnectProfile(id: String) {
        secureStore.clearProfile(id)
    }

    suspend fun deleteProfile(id: String) {
        dao.deleteLlmProfile(id)
        secureStore.clearProfile(id)
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
        dao.insertLlmUsage(
            LlmUsageEntity(
                id = UUID.randomUUID().toString(),
                profileId = profile.id,
                providerName = profile.name,
                model = result.model,
                promptTokens = usage?.promptTokens ?: 0,
                completionTokens = usage?.completionTokens ?: 0,
                totalTokens = usage?.totalTokens ?: 0,
                // Lumina never estimates or shows LLM cost; the legacy column is kept for schema compatibility.
                estimatedUsd = 0.0,
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

private fun LlmProfileEntity.toProfile(apiKey: String, linkedAccount: String): LlmProfile =
    LlmProfile(
        id = id,
        name = name,
        kind = LlmProviderKind.entries.find { it.name == kind } ?: LlmProviderKind.OPENAI_COMPAT,
        baseUrl = baseUrl,
        model = model,
        apiKey = apiKey,
        linkedAccount = linkedAccount,
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
