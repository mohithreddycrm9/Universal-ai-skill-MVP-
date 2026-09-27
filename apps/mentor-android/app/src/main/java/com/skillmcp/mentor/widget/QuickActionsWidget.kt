package com.skillmcp.mentor.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.skillmcp.mentor.MainActivity
import com.skillmcp.mentor.MentorApplication
import com.skillmcp.mentor.R
import com.skillmcp.mentor.navigation.AppLaunch
import kotlinx.coroutines.runBlocking

class QuickActionsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_quick_actions)
            views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title))
            views.setOnClickPendingIntent(R.id.widget_btn_brief, pendingUseCase(context, "daily-brief"))
            views.setOnClickPendingIntent(R.id.widget_btn_meal, pendingUseCase(context, "meal-grocery"))
            views.setOnClickPendingIntent(R.id.widget_btn_chat, pendingTab(context, "chat"))
            views.setOnClickPendingIntent(R.id.widget_btn_ask, pendingAsk(context))
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    private fun internalToken(context: Context): String {
        val container = (context.applicationContext as MentorApplication).container
        return runBlocking { container.internalLaunchToken.ensureToken() }
    }

    private fun pendingUseCase(context: Context, useCaseId: String): PendingIntent {
        val token = internalToken(context)
        val intent =
            Intent(context, MainActivity::class.java).apply {
                action = AppLaunch.ACTION_USE_CASE
                putExtra(AppLaunch.EXTRA_USE_CASE_ID, useCaseId)
                putExtra(AppLaunch.EXTRA_INTERNAL_TOKEN, token)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        return PendingIntent.getActivity(
            context,
            useCaseId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingAsk(context: Context): PendingIntent {
        val token = internalToken(context)
        val intent =
            Intent(context, MainActivity::class.java).apply {
                action = AppLaunch.ACTION_OPEN_TAB
                putExtra(AppLaunch.EXTRA_TAB_ROUTE, "chat")
                putExtra(AppLaunch.EXTRA_DRAFT, "")
                putExtra(AppLaunch.EXTRA_INTERNAL_TOKEN, token)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        return PendingIntent.getActivity(
            context,
            "ask".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingTab(context: Context, route: String): PendingIntent {
        val token = internalToken(context)
        val intent =
            Intent(context, MainActivity::class.java).apply {
                action = AppLaunch.ACTION_OPEN_TAB
                putExtra(AppLaunch.EXTRA_TAB_ROUTE, route)
                putExtra(AppLaunch.EXTRA_INTERNAL_TOKEN, token)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        return PendingIntent.getActivity(
            context,
            route.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
