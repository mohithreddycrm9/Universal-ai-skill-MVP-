package com.skillmcp.mentor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.skillmcp.mentor.data.resolvedDarkTheme
import com.skillmcp.mentor.ui.MentorApp
import com.skillmcp.mentor.ui.theme.CodeMentorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShareIntent(intent)
        val container = (application as MentorApplication).container
        setContent {
            val prefs by container.userPreferences.prefsFlow.collectAsState(
                initial = container.userPreferences.current(),
            )
            val systemDark = isSystemInDarkTheme()
            CodeMentorTheme(
                darkTheme = prefs.resolvedDarkTheme(systemDark),
                accentHue = prefs.accentHue,
                fontScale = prefs.fontScale,
            ) {
                MentorApp(container = container)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        if (!text.isNullOrEmpty()) {
            (application as MentorApplication).container.shareTextHolder.push(text)
        }
    }
}
