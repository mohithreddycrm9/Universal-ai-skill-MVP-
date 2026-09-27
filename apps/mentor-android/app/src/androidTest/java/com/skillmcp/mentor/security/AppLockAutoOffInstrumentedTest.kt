package com.skillmcp.mentor.security

import androidx.biometric.BiometricManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On devices with no screen lock, [BiometricGate] should not leave app lock enabled
 * ([resolveAppLockAuthenticators] returns [AppLockAuthenticators.canPrompt] false).
 */
@RunWith(AndroidJUnit4::class)
class AppLockAutoOffInstrumentedTest {
    @Test
    fun whenNoAuthenticators_canPromptIsFalse() {
        val auth =
            resolveAppLockAuthenticators(
                biometricStrongResult = BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
                biometricOrCredentialResult = BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            )
        assertFalse(auth.canPrompt)
        assertFalse(auth.hasBiometric)
        assertFalse(auth.hasDeviceCredential)
    }

    @Test
    fun deviceAuthenticators_matchRuntimeHelper() {
        val auth = appLockAuthenticators(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext)
        if (!auth.canPrompt) {
            assertFalse(auth.hasBiometric)
            assertFalse(auth.hasDeviceCredential)
        } else {
            assertTrue(auth.hasBiometric || auth.hasDeviceCredential)
        }
    }
}
