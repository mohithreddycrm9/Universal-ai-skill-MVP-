package com.skillmcp.mentor.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.skillmcp.mentor.ui.theme.CodeMentorTheme
import org.junit.Rule
import org.junit.Test

class ConnectBannerSnapshotTest {
    @get:Rule
    val paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_6,
            theme = "android:Theme.Material.Light.NoActionBar",
        )

    @Test
    fun connectCard_usesNeutralSurface() {
        paparazzi.snapshot {
            CodeMentorTheme(darkTheme = false) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Text(
                        "Connect OpenAI to start chatting",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}
