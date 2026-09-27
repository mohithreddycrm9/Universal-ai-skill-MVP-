package com.skillmcp.mentor.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.skillmcp.mentor.backup.BackupScheduler
import com.skillmcp.mentor.backup.BackupWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPreferencesColdStartTest {
    private lateinit var context: Context
    private lateinit var secureStore: LlmSecureStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        secureStore = LlmSecureStore(context)
        val config =
            Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun get_returnsPersistedBackupUrlBeforeCacheCollectorRuns() =
        runBlocking {
            val prefs = UserPreferences(context, secureStore)
            prefs.update { it.copy(backupUploadUrl = "https://backup.example/upload") }

            val fresh = UserPreferences(context, secureStore)
            assertEquals("", fresh.current().backupUploadUrl)
            val loaded = fresh.get()
            assertEquals("https://backup.example/upload", loaded.backupUploadUrl)
        }

    @Test
    fun coldStart_backupScheduleUsesPersistedUrl_notDefaultBlank() =
        runBlocking {
            val prefs = UserPreferences(context, secureStore)
            prefs.update { it.copy(backupUploadUrl = "https://backup.example/upload") }
            val url = prefs.get().backupUploadUrl
            assertNotEquals("", url)

            BackupScheduler.syncSchedule(context, url)

            val work =
                androidx.work.WorkManager.getInstance(context)
                    .getWorkInfosForUniqueWork(BackupWorker.UNIQUE_NAME)
                    .get()
            assertEquals(1, work.size)
            assertEquals(WorkInfo.State.ENQUEUED, work.first().state)
        }
}
