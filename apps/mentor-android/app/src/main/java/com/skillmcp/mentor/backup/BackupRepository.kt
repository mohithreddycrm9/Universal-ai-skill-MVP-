package com.skillmcp.mentor.backup

import com.skillmcp.mentor.data.LlmSecureStore
import com.skillmcp.mentor.data.UserPreferences
import com.skillmcp.mentor.data.db.MentorDao
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class BackupRepository(
    private val dao: MentorDao,
    private val encryptor: PayloadEncryptor,
    private val userPreferences: UserPreferences,
    private val secureStore: LlmSecureStore,
    private val http: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build(),
) {
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(BackupSnapshot::class.java)

    suspend fun exportEncryptedPayload(
        passphrase: CharArray? = null,
        includeApiKeys: Boolean = false,
    ): String =
        withContext(Dispatchers.IO) {
            val prefs = userPreferences.current()
            val keys =
                if (includeApiKeys || prefs.backupIncludeApiKeys) {
                    dao.allLlmProfiles().associate { row ->
                        row.id to secureStore.getKey(row.id)
                    }.filterValues { it.isNotBlank() }
                } else {
                    emptyMap()
                }
            val snapshot =
                BackupSnapshot(
                    exportedAt = System.currentTimeMillis(),
                    projects = dao.allProjects(),
                    messages = dao.allMessages(),
                    skills = dao.allSkills(),
                    apiKeys = keys,
                )
            val json = adapter.toJson(snapshot)
            if (passphrase != null && passphrase.isNotEmpty()) {
                PassphraseEncryptor.encrypt(json, passphrase)
            } else {
                encryptor.encrypt(json)
            }
        }

    suspend fun uploadIfConfigured(passphrase: CharArray? = null): Result<String> =
        withContext(Dispatchers.IO) {
            val prefs = userPreferences.current()
            val url = prefs.backupUploadUrl.trim()
            if (url.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Backup URL not configured"))
            }
            val body = exportEncryptedPayload(passphrase, prefs.backupIncludeApiKeys)
            val requestBuilder =
                Request.Builder()
                    .url(url)
                    .put(body.toRequestBody("application/octet-stream".toMediaType()))
            if (prefs.backupBearerToken.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer ${prefs.backupBearerToken}")
            }
            runCatching {
                http.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("Backup upload failed: HTTP ${response.code}")
                    }
                    "Backup uploaded (${body.length} bytes ciphertext)"
                }
            }
        }

    suspend fun restoreFromConfiguredUrl(passphrase: CharArray? = null): Result<String> =
        withContext(Dispatchers.IO) {
            val prefs = userPreferences.current()
            val url = prefs.backupUploadUrl.trim()
            if (url.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Backup URL not configured"))
            }
            val requestBuilder = Request.Builder().url(url).get()
            if (prefs.backupBearerToken.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer ${prefs.backupBearerToken}")
            }
            runCatching {
                http.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) error("Restore download failed: HTTP ${response.code}")
                    val cipher = response.body?.string() ?: error("Empty backup body")
                    val json =
                        when {
                            cipher.startsWith("v2:") -> {
                                require(passphrase != null && passphrase.isNotEmpty()) {
                                    "Passphrase required for this backup"
                                }
                                PassphraseEncryptor.decrypt(cipher, passphrase)
                            }
                            else -> encryptor.decrypt(cipher)
                        }
                    val snapshot = adapter.fromJson(json) ?: error("Invalid backup JSON")
                    snapshot.projects.forEach { dao.upsertProject(it) }
                    snapshot.messages.forEach { dao.insertMessage(it) }
                    snapshot.skills.forEach { dao.upsertSkill(it) }
                    snapshot.apiKeys.forEach { (id, key) ->
                        if (key.isNotBlank()) secureStore.setKey(id, key)
                    }
                    "Restored ${snapshot.projects.size} chats, ${snapshot.messages.size} messages"
                }
            }
        }
}
