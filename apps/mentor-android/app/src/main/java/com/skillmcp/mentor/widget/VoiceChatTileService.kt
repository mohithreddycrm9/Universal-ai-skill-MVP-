package com.skillmcp.mentor.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.skillmcp.mentor.MainActivity
import com.skillmcp.mentor.MentorApplication
import com.skillmcp.mentor.R
import com.skillmcp.mentor.navigation.AppLaunch
import com.skillmcp.mentor.navigation.InternalLaunchIntents
import kotlinx.coroutines.launch

class VoiceChatTileService : TileService() {
    override fun onStartListening() {
        qsTile?.apply {
            label = getString(R.string.voice_tile_label)
            state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = "Open chat with microphone"
            }
            updateTile()
        }
    }

    @Suppress("DEPRECATION")
    override fun onClick() {
        val app = applicationContext as MentorApplication
        app.applicationScope.launch {
            val container = app.container
            val token = container.internalLaunchToken.ensureToken()
            val intent =
                InternalLaunchIntents.mainActivity(this@VoiceChatTileService, AppLaunch.ACTION_OPEN_TAB, token) {
                    putExtra(AppLaunch.EXTRA_TAB_ROUTE, "chat")
                    putExtra(AppLaunch.EXTRA_VOICE_ON_OPEN, true)
                }
            val pending =
                PendingIntent.getActivity(
                    this@VoiceChatTileService,
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
}
