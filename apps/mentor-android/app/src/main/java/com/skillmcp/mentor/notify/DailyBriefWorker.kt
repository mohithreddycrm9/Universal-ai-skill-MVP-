package com.skillmcp.mentor.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.skillmcp.mentor.MainActivity
import com.skillmcp.mentor.R
import com.skillmcp.mentor.navigation.AppLaunch
import java.util.concurrent.TimeUnit

class DailyBriefWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        ensureChannel(applicationContext)
        val intent =
            Intent(applicationContext, MainActivity::class.java).apply {
                action = AppLaunch.ACTION_USE_CASE
                putExtra(AppLaunch.EXTRA_USE_CASE_ID, "daily-brief")
                putExtra(AppLaunch.EXTRA_INTERNAL, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val pending =
            PendingIntent.getActivity(
                applicationContext,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Morning brief")
                .setContentText("Tap to plan your top 3 tasks for today")
                .setContentIntent(pending)
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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
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
