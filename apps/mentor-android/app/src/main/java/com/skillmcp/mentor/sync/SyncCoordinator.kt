package com.skillmcp.mentor.sync

import android.content.Context
import com.skillmcp.mentor.data.UserPreferences
import com.skillmcp.mentor.data.db.MentorDao
import com.skillmcp.mentor.backup.PayloadEncryptor
import com.skillmcp.mentor.backup.BackupSnapshot
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Real-time sync via a user-provided WebSocket relay (e.g. your desktop bridge or cloud sync service).
 * Payloads are AES-GCM encrypted on device before transmission.
 */
class SyncCoordinator(
    context: Context,
    private val dao: MentorDao,
    private val encryptor: PayloadEncryptor,
    private val userPreferences: UserPreferences = UserPreferences(context),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client =
        OkHttpClient.Builder()
            .pingInterval(15, TimeUnit.SECONDS)
            .build()
    private var socket: WebSocket? = null
    private val moshi = Moshi.Builder().build()
    private val envelopeAdapter = moshi.adapter(SyncEnvelope::class.java)
    private val snapshotAdapter = moshi.adapter(BackupSnapshot::class.java)

    fun startFromPrefs() {
        scope.launch {
            val prefs = userPreferences.current()
            val url = prefs.syncWebSocketUrl.trim()
            if (url.isEmpty()) return@launch
            val deviceId = prefs.syncDeviceId.ifBlank { UUID.randomUUID().toString() }
            if (prefs.syncDeviceId.isBlank()) {
                userPreferences.update { it.copy(syncDeviceId = deviceId) }
            }
            connect(url, deviceId)
        }
    }

    fun publishStateSnapshot() {
        scope.launch {
            val ws = socket ?: return@launch
            val snapshot =
                BackupSnapshot(
                    exportedAt = System.currentTimeMillis(),
                    projects = dao.allProjects(),
                    messages = dao.allMessages(),
                    skills = dao.allSkills(),
                )
            val envelope =
                SyncEnvelope(
                    type = "snapshot",
                    deviceId = userPreferences.current().syncDeviceId,
                    cipher = encryptor.encrypt(snapshotAdapter.toJson(snapshot)),
                )
            ws.send(envelopeAdapter.toJson(envelope))
        }
    }

    private fun connect(url: String, deviceId: String) {
        socket?.close(1000, "reconnect")
        val request =
            Request.Builder()
                .url(url)
                .header("X-Device-Id", deviceId)
                .build()
        socket =
            client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        publishStateSnapshot()
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        // Relay applies merged state server-side; device treats inbound as advisory for now.
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        scope.launch {
                            kotlinx.coroutines.delay(5_000)
                            startFromPrefs()
                        }
                    }
                },
            )
    }
}
