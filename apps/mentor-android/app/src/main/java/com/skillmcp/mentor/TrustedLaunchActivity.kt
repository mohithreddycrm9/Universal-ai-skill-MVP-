package com.skillmcp.mentor

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.skillmcp.mentor.navigation.AppLaunch
import kotlinx.coroutines.launch

/**
 * Trampoline for static shortcuts and notification taps: attaches the device-local launch token
 * then forwards to [MainActivity].
 */
class TrustedLaunchActivity : androidx.activity.ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as MentorApplication).container
        lifecycleScope.launch {
            val token = container.internalLaunchToken.ensureToken()
            val forward =
                Intent(this@TrustedLaunchActivity, MainActivity::class.java).apply {
                    action = intent.action
                    data = intent.data
                    intent.extras?.let { putExtras(it) }
                    putExtra(AppLaunch.EXTRA_INTERNAL_TOKEN, token)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            startActivity(forward)
            finish()
        }
    }
}
