package com.skillmcp.mentor.data

import com.skillmcp.mentor.data.db.MentorDao
import com.skillmcp.mentor.llm.LlmProfileRepository

class LocalDataWiper(
    private val dao: MentorDao,
    private val secureStore: LlmSecureStore,
    private val userPreferences: UserPreferences,
    private val llmProfileRepository: LlmProfileRepository,
) {
    suspend fun wipeAllUserContent() {
        dao.wipeUserTables()
        dao.allLlmProfiles().forEach { profile ->
            llmProfileRepository.disconnectProfile(profile.id)
        }
        secureStore.wipeAll()
        userPreferences.resetToDefaults()
    }
}
