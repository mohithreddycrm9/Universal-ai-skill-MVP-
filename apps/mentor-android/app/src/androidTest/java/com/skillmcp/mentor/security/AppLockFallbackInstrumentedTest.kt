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
        assertFalse(auth.label.isBlank())
        assertTrue(
            auth.label.contains("biometric", ignoreCase = true) ||
                auth.label.contains("screen lock", ignoreCase = true) ||
                auth.label.contains("PIN", ignoreCase = true),
        )
    }
}
