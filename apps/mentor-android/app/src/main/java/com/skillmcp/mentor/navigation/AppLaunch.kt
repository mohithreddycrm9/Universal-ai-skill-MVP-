package com.skillmcp.mentor.navigation

object AppLaunch {
    const val ACTION_USE_CASE = "com.skillmcp.mentor.USE_CASE"
    const val ACTION_OPEN_TAB = "com.skillmcp.mentor.OPEN_TAB"
    const val EXTRA_USE_CASE_ID = "use_case_id"
    const val EXTRA_TAB_ROUTE = "tab_route"
    const val EXTRA_DRAFT = "draft"
    /** Set on intents created by this app (widget, shortcuts, notifications). */
    const val EXTRA_INTERNAL = "internal_launch"
    const val EXTRA_VOICE_ON_OPEN = "voice_on_open"
    const val EXTRA_INTERNAL_TOKEN = "internal_launch_token"
}
