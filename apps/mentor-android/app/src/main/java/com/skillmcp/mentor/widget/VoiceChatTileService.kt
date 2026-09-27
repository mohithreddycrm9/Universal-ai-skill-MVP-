package com.skillmcp.mentor.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.skillmcp.mentor.MainActivity
import com.skillmcp.mentor.MentorApplication
import com.skillmcp.mentor.navigation.AppLaunch
import kotlinx.coroutines.runBlocking

class VoiceChatTileService : TileService() {
    override fun onStartListening() {
        qsTile?.apply {
            label = "Universal AI voice"
            state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = "Open chat with microphone"
            }
            updateTile()
        }
    }

    override fun onClick() {
        val container = (applicationContext as MentorApplication).container
        val token = runBlocking { container.internalLaunchToken.ensureToken() }
        val intent =
            Intent(this, MainActivity::class.java).apply {
                action = AppLaunch.ACTION_OPEN_TAB
                putExtra(AppLaunch.EXTRA_TAB_ROUTE, "chat")
                putExtra(AppLaunch.EXTRA_VOICE_ON_OPEN, true)
                putExtra(AppLaunch.EXTRA_INTERNAL_TOKEN, token)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val pending =
            PendingIntent.getActivity(
                this,
                77,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        unlockAndRun {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(pending)
            } else {
                @SuppressLint("StartActivityAndCollapseDeprecated")
                startActivityAndCollapse(intent)
            }
        }
    }
}
