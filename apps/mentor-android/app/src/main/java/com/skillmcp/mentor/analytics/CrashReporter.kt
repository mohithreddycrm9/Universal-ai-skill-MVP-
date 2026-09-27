package com.skillmcp.mentor.analytics

import android.content.Context
import android.util.Log

/**
 * Opt-in crash reporting hook. Wire Firebase Crashlytics or Sentry here when SDK keys are configured.
 */
object CrashReporter {
    private const val TAG = "MentorCrash"

    fun install(app: android.app.Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isEnabled(app)) {
                Log.e(TAG, "Uncaught on ${thread.name}", throwable)
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun recordNonFatal(context: Context, message: String, throwable: Throwable? = null) {
        if (!isEnabled(context)) return
        Log.e(TAG, message, throwable)
    }

    fun isEnabled(context: Context): Boolean {
        val prefs = (context.applicationContext as? com.skillmcp.mentor.MentorApplication)?.container?.userPreferences
        return prefs?.current()?.crashReportingOptIn == true
    }
}
