package com.skillmcp.mentor.security

import org.junit.Assert.assertFalse
import org.junit.Test

class AppLockAuthenticatorsTest {
    @Test
    fun authenticatorsDataClassDefaults() {
        val auth = AppLockAuthenticators(allowed = 0, hasBiometric = false, hasDeviceCredential = false)
        assertFalse(auth.canPrompt)
    }
}
