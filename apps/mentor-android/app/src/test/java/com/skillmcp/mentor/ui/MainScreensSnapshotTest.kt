package com.skillmcp.mentor.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.skillmcp.mentor.ui.theme.CodeMentorTheme
import org.junit.Rule
import org.junit.Test

class MainScreensSnapshotTest {
    @get:Rule
    val paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_6,
            theme = "android:Theme.Material.Light.NoActionBar",
        )

    @Test
    fun chatHeader_light() {
        paparazzi.snapshot {
            CodeMentorTheme(darkTheme = false) {
                Text(
                    "Universal AI — Chat",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }

    @Test
    fun settingsHeader_dark() {
        paparazzi.snapshot {
            CodeMentorTheme(darkTheme = true) {
                Text(
                    "Settings",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }

    @Test
    fun discoverHeader_light() {
        paparazzi.snapshot {
            CodeMentorTheme(darkTheme = false) {
                Text(
                    "Discover",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}
