package com.skillmcp.mentor.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BackupScheduler {
    fun syncSchedule(context: Context, backupUploadUrl: String) {
        val workManager = WorkManager.getInstance(context)
        if (backupUploadUrl.isBlank()) {
            workManager.cancelUniqueWork(BackupWorker.UNIQUE_NAME)
            return
        }
        val request =
            PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
                .build()
        workManager.enqueueUniquePeriodicWork(
            BackupWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
