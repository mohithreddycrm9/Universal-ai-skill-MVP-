package com.skillmcp.mentor.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skillmcp.mentor.MentorApplication

class BackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as MentorApplication).container
        return container.backupRepository.uploadIfConfigured().fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }

    companion object {
        const val UNIQUE_NAME = "mentor_daily_backup"
    }
}
