package com.skillmcp.mentor.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.skillmcp.mentor.ui.preview.ChatScreenPreview
import com.skillmcp.mentor.ui.preview.DiscoverScreenPreview
import com.skillmcp.mentor.ui.preview.SettingsScreenPreview
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
    fun chatScreen_light() {
        paparazzi.snapshot { ChatScreenPreview(darkTheme = false) }
    }

    @Test
    fun chatScreen_dark() {
        paparazzi.snapshot { ChatScreenPreview(darkTheme = true) }
    }

    @Test
    fun discoverScreen_light() {
        paparazzi.snapshot { DiscoverScreenPreview(darkTheme = false) }
    }

    @Test
    fun discoverScreen_dark() {
        paparazzi.snapshot { DiscoverScreenPreview(darkTheme = true) }
    }

    @Test
    fun settingsScreen_light() {
        paparazzi.snapshot { SettingsScreenPreview(darkTheme = false) }
    }

    @Test
    fun settingsScreen_dark() {
        paparazzi.snapshot { SettingsScreenPreview(darkTheme = true) }
    }
}
