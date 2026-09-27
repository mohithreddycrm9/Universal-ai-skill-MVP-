package com.skillmcp.mentor.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.skillmcp.mentor.MentorApplication
import com.skillmcp.mentor.R
import com.skillmcp.mentor.navigation.AppLaunch
import com.skillmcp.mentor.navigation.InternalLaunchIntents
import java.util.concurrent.TimeUnit

class DailyBriefWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        ensureChannel(applicationContext)
        val container = (applicationContext as MentorApplication).container
        val prefs = container.userPreferences.get()
        val brief = container.morningBriefCollector.collect(prefs)
        val body = brief.lines.joinToString("\n")
        val launchToken = container.internalLaunchToken.ensureToken()
        val openBriefIntent =
            InternalLaunchIntents.mainActivity(
                applicationContext,
                AppLaunch.ACTION_USE_CASE,
                launchToken,
            ) {
                putExtra(AppLaunch.EXTRA_USE_CASE_ID, "daily-brief")
            }
        val continueIntent =
            InternalLaunchIntents.mainActivity(
                applicationContext,
                AppLaunch.ACTION_OPEN_TAB,
                launchToken,
            ) {
                putExtra(AppLaunch.EXTRA_TAB_ROUTE, "chat")
                putExtra(AppLaunch.EXTRA_DRAFT, brief.chatPrompt)
            }
        val openPending =
            PendingIntent.getActivity(
                applicationContext,
                1001,
                openBriefIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val continuePending =
            PendingIntent.getActivity(
                applicationContext,
                1002,
                continueIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Morning brief")
                .setContentText(body.lines().firstOrNull() ?: "Your brief is ready")
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(openPending)
                .addAction(0, "Continue in chat", continuePending)
                .setAutoCancel(true)
                .build()
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "daily_brief"
        private const val NOTIFICATION_ID = 42
        private const val WORK_NAME = "daily_brief_reminder"

        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Daily brief",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Optional reminder to run your morning brief"
                }
            nm.createNotificationChannel(channel)
        }

        fun schedule(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<DailyBriefWorker>(24, TimeUnit.HOURS)
                    .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
