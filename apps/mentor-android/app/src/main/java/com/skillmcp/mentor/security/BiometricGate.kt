package com.skillmcp.mentor.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

data class AppLockAuthenticators(
    val allowed: Int,
    val hasBiometric: Boolean,
    val hasDeviceCredential: Boolean,
) {
    val canPrompt: Boolean = allowed != 0
}

fun appLockAuthenticators(context: Context): AppLockAuthenticators {
    val manager = BiometricManager.from(context)
    val hasBiometric =
        manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    val hasDeviceCredential =
        manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        ) == BiometricManager.BIOMETRIC_SUCCESS
    val allowed =
        when {
            hasBiometric -> BiometricManager.Authenticators.BIOMETRIC_STRONG
            hasDeviceCredential -> BiometricManager.Authenticators.DEVICE_CREDENTIAL
            else -> 0
        }
    return AppLockAuthenticators(allowed = allowed, hasBiometric = hasBiometric, hasDeviceCredential = hasDeviceCredential)
}

@Composable
fun BiometricGate(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var unlocked by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val auth = remember { appLockAuthenticators(context) }

    DisposableEffect(lifecycleOwner, enabled) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (enabled && event == Lifecycle.Event.ON_STOP) {
                    unlocked = false
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun showPrompt() {
        if (activity == null || !auth.canPrompt) return
        val executor = ContextCompat.getMainExecutor(context)
        val prompt =
            BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        unlocked = true
                    }
                },
            )
        val info =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Universal AI")
                .setSubtitle("Confirm it's you to open chats and API keys")
                .setAllowedAuthenticators(auth.allowed)
                .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(enabled, activity, auth.allowed) {
        if (!enabled || unlocked || activity == null || !auth.canPrompt) {
            return@LaunchedEffect
        }
        showPrompt()
    }

    if (unlocked) {
        content()
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Locked", style = MaterialTheme.typography.headlineSmall)
            Text(
                when {
                    auth.hasBiometric -> "Use fingerprint or face to continue."
                    auth.hasDeviceCredential -> "Use your device PIN, pattern, or password to continue."
                    else ->
                        "Add a screen lock in Android Settings before using app lock."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
            if (auth.canPrompt && activity != null) {
                Button(onClick = { showPrompt() }) {
                    Text(if (auth.hasBiometric) "Unlock" else "Unlock with device PIN")
                }
            }
        }
    }
}
