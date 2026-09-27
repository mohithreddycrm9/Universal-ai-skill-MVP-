package com.skillmcp.mentor.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLockFallbackInstrumentedTest {
    @Test
    fun authenticatorsReportDeviceLockState() {
        val auth = appLockAuthenticators(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext)
        assertTrue(auth.hasBiometric || auth.hasDeviceCredential || !auth.canPrompt)
        if (auth.canPrompt) {
            assertTrue(auth.hasBiometric || auth.hasDeviceCredential)
        } else {
            assertFalse(auth.hasBiometric)
            assertFalse(auth.hasDeviceCredential)
        }
    }
}
