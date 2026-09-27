@file:Suppress("DEPRECATION")

package com.skillmcp.mentor.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class LlmSecureStore(context: Context) {
    private val prefs =
        EncryptedSharedPreferences.create(
            context,
            "llm_api_keys",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    fun getKey(profileId: String): String = prefs.getString(keyFor(profileId), "") ?: ""

    fun setKey(profileId: String, apiKey: String) {
        prefs.edit().putString(keyFor(profileId), apiKey).apply()
    }

    fun deleteKey(profileId: String) {
        prefs.edit().remove(keyFor(profileId)).apply()
    }

    fun getLinkedAccount(profileId: String): String = prefs.getString(linkedKeyFor(profileId), "") ?: ""

    fun setLinkedAccount(profileId: String, account: String) {
        if (account.isBlank()) {
            prefs.edit().remove(linkedKeyFor(profileId)).apply()
        } else {
            prefs.edit().putString(linkedKeyFor(profileId), account).apply()
        }
    }

    fun clearProfile(profileId: String) {
        prefs.edit().remove(keyFor(profileId)).remove(linkedKeyFor(profileId)).apply()
    }

    fun getAppSecret(secretId: String): String = prefs.getString(appSecretKey(secretId), "") ?: ""

    fun setAppSecret(secretId: String, value: String) {
        if (value.isBlank()) {
            prefs.edit().remove(appSecretKey(secretId)).apply()
        } else {
            prefs.edit().putString(appSecretKey(secretId), value).apply()
        }
    }

    companion object {
        const val SECRET_ELEVEN_LABS = "eleven_labs_api_key"
        const val SECRET_BACKUP_TOKEN = "backup_bearer_token"
    }

    private fun keyFor(profileId: String): String = "profile_key_$profileId"

    private fun linkedKeyFor(profileId: String): String = "profile_linked_$profileId"

    fun wipeAll() {
        prefs.edit().clear().apply()
    }

    private fun appSecretKey(secretId: String): String = "app_secret_$secretId"
}
