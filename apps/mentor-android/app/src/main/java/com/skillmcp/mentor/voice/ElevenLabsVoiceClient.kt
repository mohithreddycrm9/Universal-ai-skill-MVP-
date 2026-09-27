package com.skillmcp.mentor.voice

import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Optional cloned-voice playback via ElevenLabs. User supplies API key + voice id in Settings.
 */
class ElevenLabsVoiceClient(
    private val apiKey: String,
    private val voiceId: String,
    private val cacheDir: File,
    private val http: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build(),
) {
    suspend fun speak(text: String, localeTag: String): Boolean =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank() || voiceId.isBlank()) return@withContext false
            val body =
                """
                {"text":${text.quoteJson()},"model_id":"eleven_multilingual_v2","language_code":"${localeTag.take(2)}"}
                """.trimIndent()
            val request =
                Request.Builder()
                    .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
                    .addHeader("xi-api-key", apiKey)
                    .addHeader("Accept", "audio/mpeg")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
            val bytes =
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext false
                    response.body?.bytes() ?: return@withContext false
                }
            val file = File(cacheDir, "voice-${System.currentTimeMillis()}.mp3")
            file.writeBytes(bytes)
            withContext(Dispatchers.Main) {
                MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        release()
                        file.delete()
                    }
                }
            }
            true
        }

    private fun String.quoteJson(): String =
        "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}
