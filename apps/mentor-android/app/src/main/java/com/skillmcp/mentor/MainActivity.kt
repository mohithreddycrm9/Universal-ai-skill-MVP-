package com.skillmcp.mentor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.skillmcp.mentor.data.LaunchAction
import com.skillmcp.mentor.data.resolvedDarkTheme
import com.skillmcp.mentor.navigation.AppLaunch
import com.skillmcp.mentor.navigation.LaunchAllowlist
import com.skillmcp.mentor.ui.MentorApp
import com.skillmcp.mentor.ui.theme.CodeMentorTheme

class MainActivity : AppCompatActivity() {
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
        val trusted = isTrustedInternalLaunch(intent, container)
        when (intent.action) {
            AppLaunch.ACTION_USE_CASE -> {
                val id = intent.getStringExtra(AppLaunch.EXTRA_USE_CASE_ID)
                if (id.isNullOrBlank()) return
                if (trusted || LaunchAllowlist.isAllowedUseCase(id)) {
                    container.launchIntentHolder.push(LaunchAction.UseCase(id))
                }
            }
            AppLaunch.ACTION_OPEN_TAB -> {
                val tab = intent.getStringExtra(AppLaunch.EXTRA_TAB_ROUTE) ?: "chat"
                val voiceOnOpen = intent.getBooleanExtra(AppLaunch.EXTRA_VOICE_ON_OPEN, false)
                val hasDraft = !intent.getStringExtra(AppLaunch.EXTRA_DRAFT).isNullOrBlank()
                when {
                    trusted -> {
                        container.launchIntentHolder.push(LaunchAction.OpenTab(tab))
                        if (voiceOnOpen) {
                            container.launchIntentHolder.push(LaunchAction.VoiceChat)
                        }
                    }
                    LaunchAllowlist.isAllowedTabRoute(tab) && !voiceOnOpen && !hasDraft -> {
                        container.launchIntentHolder.push(LaunchAction.OpenTab(tab))
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                val uri: Uri? = intent.data
                if (uri?.scheme == "universalai" && uri.host == "usecase") {
                    val id = uri.lastPathSegment
                    if (!id.isNullOrBlank() && LaunchAllowlist.isAllowedUseCase(id)) {
                        container.launchIntentHolder.push(LaunchAction.UseCase(id))
                    }
                }
            }
        }
        if (trusted) {
            val draft = intent.getStringExtra(AppLaunch.EXTRA_DRAFT)
            if (!draft.isNullOrBlank()) {
                container.launchIntentHolder.push(LaunchAction.Draft(draft))
            }
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

    private fun isTrustedInternalLaunch(
        intent: Intent,
        container: com.skillmcp.mentor.data.AppContainer,
    ): Boolean {
        val token = container.userPreferences.current().internalLaunchToken
        return container.internalLaunchToken.matches(intent, token)
    }
}
