package com.skillmcp.mentor.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the app's Baseline Profile (startup + chat scroll + drawer).
 * Needs a device/emulator on API 33+ (or rooted 28+):
 *   ./gradlew :app:generateReleaseBaselineProfile
 * Output is written to app/src/release/generated/baselineProfiles/.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(packageName = TARGET_PACKAGE, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            scrollChatList()
            openAndCloseDrawer()
        }
}
