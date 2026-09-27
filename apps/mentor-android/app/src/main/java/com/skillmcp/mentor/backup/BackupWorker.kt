package com.skillmcp.mentor.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skillmcp.mentor.MentorApplication
import java.io.IOException

class BackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as MentorApplication).container
        val prefs = container.userPreferences.get()
        if (prefs.backupUploadUrl.isBlank()) {
            return Result.success()
        }
        return container.backupRepository.uploadIfConfigured().fold(
            onSuccess = { Result.success() },
            onFailure = { err ->
                if (err is IOException) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            },
        )
    }

    companion object {
        const val UNIQUE_NAME = "mentor_daily_backup"
    }
}
