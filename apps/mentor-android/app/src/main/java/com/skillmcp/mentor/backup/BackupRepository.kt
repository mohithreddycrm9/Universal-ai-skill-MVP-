package com.skillmcp.mentor.backup

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
    private val http: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build(),
) {
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(BackupSnapshot::class.java)

    suspend fun exportEncryptedPayload(): String =
        withContext(Dispatchers.IO) {
            val snapshot =
                BackupSnapshot(
                    exportedAt = System.currentTimeMillis(),
                    projects = dao.allProjects(),
                    messages = dao.allMessages(),
                    skills = dao.allSkills(),
                )
            encryptor.encrypt(adapter.toJson(snapshot))
        }

    suspend fun uploadIfConfigured(): Result<String> =
        withContext(Dispatchers.IO) {
            val prefs = userPreferences.current()
            val url = prefs.backupUploadUrl.trim()
            if (url.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Backup URL not configured"))
            }
            val body = exportEncryptedPayload()
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
}
