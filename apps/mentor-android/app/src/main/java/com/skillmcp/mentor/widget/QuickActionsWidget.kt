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
import com.skillmcp.mentor.navigation.InternalLaunchIntents
import kotlinx.coroutines.launch

class QuickActionsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        val app = context.applicationContext as MentorApplication
        app.applicationScope.launch {
            try {
                val container = app.container
                val token = container.internalLaunchToken.ensureToken()
                appWidgetIds.forEach { id ->
                    val views = RemoteViews(context.packageName, R.layout.widget_quick_actions)
                    views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title))
                    views.setOnClickPendingIntent(R.id.widget_btn_brief, pendingUseCase(context, "daily-brief", token))
                    views.setOnClickPendingIntent(R.id.widget_btn_meal, pendingUseCase(context, "meal-grocery", token))
                    views.setOnClickPendingIntent(R.id.widget_btn_chat, pendingTab(context, "chat", token))
                    views.setOnClickPendingIntent(R.id.widget_btn_ask, pendingAsk(context, token))
                    appWidgetManager.updateAppWidget(id, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun pendingUseCase(context: Context, useCaseId: String, token: String): PendingIntent {
        val intent =
            InternalLaunchIntents.mainActivity(context, AppLaunch.ACTION_USE_CASE, token) {
                putExtra(AppLaunch.EXTRA_USE_CASE_ID, useCaseId)
            }
        return PendingIntent.getActivity(
            context,
            useCaseId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingAsk(context: Context, token: String): PendingIntent {
        val intent =
            InternalLaunchIntents.mainActivity(context, AppLaunch.ACTION_OPEN_TAB, token) {
                putExtra(AppLaunch.EXTRA_TAB_ROUTE, "chat")
                putExtra(AppLaunch.EXTRA_VOICE_ON_OPEN, true)
            }
        return PendingIntent.getActivity(
            context,
            "ask".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingTab(context: Context, route: String, token: String): PendingIntent {
        val intent =
            InternalLaunchIntents.mainActivity(context, AppLaunch.ACTION_OPEN_TAB, token) {
                putExtra(AppLaunch.EXTRA_TAB_ROUTE, route)
            }
        return PendingIntent.getActivity(
            context,
            route.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
