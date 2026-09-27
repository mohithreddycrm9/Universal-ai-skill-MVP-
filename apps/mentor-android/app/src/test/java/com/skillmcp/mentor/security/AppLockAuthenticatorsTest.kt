package com.skillmcp.mentor.security

import androidx.biometric.BiometricManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockAuthenticatorsTest {
    @Test
    fun resolve_prefersBiometricOverCredential() {
        val auth =
            resolveAppLockAuthenticators(
                biometricStrongResult = BiometricManager.BIOMETRIC_SUCCESS,
                biometricOrCredentialResult = BiometricManager.BIOMETRIC_SUCCESS,
            )
        assertTrue(auth.canPrompt)
        assertTrue(auth.hasBiometric)
        assertEquals(BiometricManager.Authenticators.BIOMETRIC_STRONG, auth.allowed)
    }

    @Test
    fun resolve_deviceCredentialOnly() {
        val auth =
            resolveAppLockAuthenticators(
                biometricStrongResult = BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
                biometricOrCredentialResult = BiometricManager.BIOMETRIC_SUCCESS,
            )
        assertTrue(auth.canPrompt)
        assertFalse(auth.hasBiometric)
        assertTrue(auth.hasDeviceCredential)
        assertEquals(BiometricManager.Authenticators.DEVICE_CREDENTIAL, auth.allowed)
    }

    @Test
    fun resolve_noLockAvailable() {
        val auth =
            resolveAppLockAuthenticators(
                biometricStrongResult = BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                biometricOrCredentialResult = BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            )
        assertFalse(auth.canPrompt)
        assertEquals(0, auth.allowed)
    }
}
