package com.skillmcp.mentor.data

import android.content.Intent
import com.skillmcp.mentor.navigation.AppLaunch
import java.util.UUID

class InternalLaunchToken(
    private val userPreferences: UserPreferences,
) {
    suspend fun ensureToken(): String {
        val current = userPreferences.get().internalLaunchToken
        if (current.isNotBlank()) return current
        val token = UUID.randomUUID().toString()
        userPreferences.update { it.copy(internalLaunchToken = token) }
        return token
    }

    fun matches(intent: Intent?, expected: String): Boolean {
        if (intent == null || expected.isBlank()) return false
        return intent.getStringExtra(AppLaunch.EXTRA_INTERNAL_TOKEN) == expected
    }
}
