package com.skillmcp.mentor.llm

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GoogleLlmSignIn {
    suspend fun signIn(context: Context, webClientId: String): Result<String> =
        withContext(Dispatchers.Main) {
            if (webClientId.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Google sign-in is not configured for this build"))
            }
            runCatching {
                val option =
                    GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val credential = CredentialManager.create(context).getCredential(context, request).credential
                val googleId =
                    when (credential) {
                        is CustomCredential ->
                            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                GoogleIdTokenCredential.createFrom(credential.data)
                            } else {
                                null
                            }
                        else -> null
                    }
                val email = googleId?.id ?: throw IllegalStateException("Could not read Google account")
                email
            }.recoverCatching { error ->
                if (error is GetCredentialException) {
                    throw IllegalStateException(error.message ?: "Google sign-in cancelled")
                } else {
                    throw error
                }
            }
        }
}
