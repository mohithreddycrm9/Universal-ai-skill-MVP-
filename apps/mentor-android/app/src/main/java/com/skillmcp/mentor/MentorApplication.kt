package com.skillmcp.mentor

import android.app.Application
import com.skillmcp.mentor.analytics.CrashReporter
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.skillmcp.mentor.backup.BackupScheduler
import com.skillmcp.mentor.data.AppContainer
import com.skillmcp.mentor.util.applyAppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MentorApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        PDFBoxResourceLoader.init(applicationContext)
        container = AppContainer(this)
        runBlocking { container.internalLaunchToken.ensureToken() }
        applyAppLanguage(container.userPreferences.current().appLanguageTag)
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            container.userPreferences.prefsFlow
                .map { it.appLanguageTag }
                .distinctUntilChanged()
                .collect { applyAppLanguage(it) }
        }
        BackupScheduler.syncSchedule(
            this,
            container.userPreferences.current().backupUploadUrl,
        )
    }
}
