package com.skillmcp.mentor

import android.app.Application
import com.skillmcp.mentor.backup.BackupScheduler
import com.skillmcp.mentor.data.AppContainer

class MentorApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        BackupScheduler.syncSchedule(
            this,
            container.userPreferences.current().backupUploadUrl,
        )
    }
}
