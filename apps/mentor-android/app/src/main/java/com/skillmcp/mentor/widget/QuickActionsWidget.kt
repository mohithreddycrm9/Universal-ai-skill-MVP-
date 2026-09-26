package com.skillmcp.mentor.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.skillmcp.mentor.MainActivity
import com.skillmcp.mentor.R
import com.skillmcp.mentor.navigation.AppLaunch

class QuickActionsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_quick_actions)
            views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title))
            views.setOnClickPendingIntent(R.id.widget_btn_brief, pendingUseCase(context, "daily-brief"))
            views.setOnClickPendingIntent(R.id.widget_btn_meal, pendingUseCase(context, "meal-grocery"))
            views.setOnClickPendingIntent(R.id.widget_btn_chat, pendingTab(context, "chat"))
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    private fun pendingUseCase(context: Context, useCaseId: String): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                action = AppLaunch.ACTION_USE_CASE
                putExtra(AppLaunch.EXTRA_USE_CASE_ID, useCaseId)
                putExtra(AppLaunch.EXTRA_INTERNAL, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        return PendingIntent.getActivity(
            context,
            useCaseId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingTab(context: Context, route: String): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                action = AppLaunch.ACTION_OPEN_TAB
                putExtra(AppLaunch.EXTRA_TAB_ROUTE, route)
                putExtra(AppLaunch.EXTRA_INTERNAL, true)
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
