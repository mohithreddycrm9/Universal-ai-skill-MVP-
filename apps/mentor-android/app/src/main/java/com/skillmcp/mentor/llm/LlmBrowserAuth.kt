package com.skillmcp.mentor.llm

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object LlmBrowserAuth {
    fun openSignIn(context: Context, destination: LlmSignInDestination) {
        val intent = CustomTabsIntent.Builder().setShowTitle(true).build()
        intent.launchUrl(context, Uri.parse(destination.url))
    }

    fun destinationFor(info: LlmProviderConnectInfo, method: LlmSignInMethod): LlmSignInDestination? =
        info.signInDestinations.find { it.method == method }
            ?: when (method) {
                LlmSignInMethod.GOOGLE ->
                    info.signInDestinations.firstOrNull { it.method == LlmSignInMethod.GOOGLE }
                LlmSignInMethod.EMAIL ->
                    info.signInDestinations.firstOrNull { it.method == LlmSignInMethod.EMAIL }
                LlmSignInMethod.PHONE ->
                    info.signInDestinations.firstOrNull { it.method == LlmSignInMethod.PHONE }
                LlmSignInMethod.API_KEY -> null
            }
}
