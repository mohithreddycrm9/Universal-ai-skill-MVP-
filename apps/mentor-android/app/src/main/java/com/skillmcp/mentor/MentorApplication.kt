package com.skillmcp.mentor

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.skillmcp.mentor.backup.BackupWorker
import com.skillmcp.mentor.data.AppContainer
import java.util.concurrent.TimeUnit

class MentorApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        scheduleDailyBackup()
    }

    private fun scheduleDailyBackup() {
        val request =
            PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
                .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            BackupWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
