package com.skillmcp.mentor.navigation

import android.content.Context
import android.content.Intent
import com.skillmcp.mentor.MainActivity

object InternalLaunchIntents {
    fun mainActivity(
        context: Context,
        action: String,
        token: String,
        configure: Intent.() -> Unit = {},
    ): Intent =
        Intent(context, MainActivity::class.java).apply {
            this.action = action
            putExtra(AppLaunch.EXTRA_INTERNAL_TOKEN, token)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            configure()
        }
}
