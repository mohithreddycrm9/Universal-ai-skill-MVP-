package com.skillmcp.mentor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import com.skillmcp.mentor.data.LaunchAction
import com.skillmcp.mentor.data.resolvedDarkTheme
import com.skillmcp.mentor.navigation.AppLaunch
import com.skillmcp.mentor.ui.MentorApp
import com.skillmcp.mentor.ui.theme.CodeMentorTheme
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MentorApplication).container
        if (savedInstanceState == null) {
            routeLaunchIntent(intent, container)
            handleShareIntent(intent, container)
        }
        setContent {
            val prefs by container.userPreferences.prefsFlow.collectAsState(
                initial = container.userPreferences.current(),
            )
            val systemDark = isSystemInDarkTheme()
            CodeMentorTheme(
                darkTheme = prefs.resolvedDarkTheme(systemDark),
                accentHue = prefs.accentHue,
                fontScale = prefs.fontScale,
                dynamicColor = prefs.useDynamicColor,
            ) {
                MentorApp(container = container)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val container = (application as MentorApplication).container
        routeLaunchIntent(intent, container)
        handleShareIntent(intent, container)
    }

    private fun routeLaunchIntent(intent: Intent?, container: com.skillmcp.mentor.data.AppContainer) {
        if (intent == null) return
        when (intent.action) {
            AppLaunch.ACTION_USE_CASE -> {
                if (!isTrustedInternalLaunch(intent, container)) return
                val id = intent.getStringExtra(AppLaunch.EXTRA_USE_CASE_ID)
                if (!id.isNullOrBlank()) {
                    container.launchIntentHolder.push(LaunchAction.UseCase(id))
                }
            }
            AppLaunch.ACTION_OPEN_TAB -> {
                if (!isTrustedInternalLaunch(intent, container)) return
                val tab = intent.getStringExtra(AppLaunch.EXTRA_TAB_ROUTE) ?: "chat"
                container.launchIntentHolder.push(LaunchAction.OpenTab(tab))
                if (intent.getBooleanExtra(AppLaunch.EXTRA_VOICE_ON_OPEN, false)) {
                    container.launchIntentHolder.push(LaunchAction.VoiceChat)
                }
            }
            Intent.ACTION_VIEW -> {
                val uri: Uri? = intent.data
                if (uri?.scheme == "universalai" && uri.host == "usecase") {
                    val id = uri.lastPathSegment
                    if (!id.isNullOrBlank()) {
                        container.launchIntentHolder.push(LaunchAction.UseCase(id))
                    }
                }
            }
        }
        val draft = intent.getStringExtra(AppLaunch.EXTRA_DRAFT)
        if (!draft.isNullOrBlank() && isTrustedInternalLaunch(intent, container)) {
            container.launchIntentHolder.push(LaunchAction.Draft(draft))
        }
    }

    private fun handleShareIntent(intent: Intent?, container: com.skillmcp.mentor.data.AppContainer) {
        if (intent?.action != Intent.ACTION_SEND) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        if (!text.isNullOrEmpty()) {
            val enriched =
                if (text.startsWith("http://") || text.startsWith("https://")) {
                    "Shared link: $text\n\nSummarize this page and list key takeaways. " +
                        "(Enable the fetch plugin and use /fetch if you want raw text.)"
                } else {
                    text
                }
            container.shareTextHolder.push(
                com.skillmcp.mentor.data.SharePayload(
                    text = enriched,
                    sendsToAiProvider = true,
                ),
            )
        }
    }

    /** Widget, tile, and notification PendingIntents include a secret token stored on device. */
    private fun isTrustedInternalLaunch(
        intent: Intent,
        container: com.skillmcp.mentor.data.AppContainer,
    ): Boolean {
        val token = container.userPreferences.current().internalLaunchToken
        return container.internalLaunchToken.matches(intent, token)
    }
}
