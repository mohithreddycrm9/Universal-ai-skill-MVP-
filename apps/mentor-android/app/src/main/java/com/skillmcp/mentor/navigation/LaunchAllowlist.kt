package com.skillmcp.mentor.navigation

/**
 * External entry points (launcher shortcuts) may only trigger these actions without a device token.
 * Widgets, tiles, and notifications must use [AppLaunch.EXTRA_INTERNAL_TOKEN] from a PendingIntent
 * created inside this app.
 */
object LaunchAllowlist {
    val shortcutUseCaseIds: Set<String> =
        setOf(
            "daily-brief",
            "rewrite-message",
            "meal-grocery",
        )

    val shortcutTabRoutes: Set<String> =
        setOf(
            "chat",
        )

    fun isAllowedUseCase(id: String?): Boolean = !id.isNullOrBlank() && id in shortcutUseCaseIds

    fun isAllowedTabRoute(route: String?): Boolean = route in shortcutTabRoutes
}
