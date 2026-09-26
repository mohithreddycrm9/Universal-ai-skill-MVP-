package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.R
import com.skillmcp.mentor.llm.HuggingFaceDefaults
import com.skillmcp.mentor.llm.LlmBrowserAuth
import com.skillmcp.mentor.llm.LlmProfile
import com.skillmcp.mentor.llm.LlmProviderKind
import com.skillmcp.mentor.llm.LlmSignInMethod
import com.skillmcp.mentor.llm.connectInfoFor
import com.skillmcp.mentor.llm.isConfigured
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.llm.connectionLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConnectLlmSheet(
    profile: LlmProfile?,
    vm: MentorViewModel,
    onDismiss: () -> Unit,
) {
    if (profile == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val info = remember(profile.kind) { connectInfoFor(profile.kind) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val webClientId = remember { context.getString(R.string.google_web_client_id) }

    var method by remember(profile.id) {
        mutableStateOf(info.signInMethods.first())
    }
    var apiKey by remember(profile.id) { mutableStateOf(profile.apiKey) }
    var linked by remember(profile.id) { mutableStateOf(profile.linkedAccount) }
    var baseUrl by remember(profile.id) { mutableStateOf(profile.baseUrl) }
    var model by remember(profile.id) { mutableStateOf(profile.model) }
    var showKey by remember { mutableStateOf(false) }
    var browserHint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(profile) {
        apiKey = profile.apiKey
        linked = profile.linkedAccount
        baseUrl = profile.baseUrl
        model = profile.model
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(profile.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(info.headline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val draftConfigured =
                remember(apiKey, baseUrl, model, profile.kind) {
                    profile.copy(apiKey = apiKey.trim(), baseUrl = baseUrl, model = model).isConfigured()
                }
            Text(
                profile.copy(apiKey = apiKey.trim(), linkedAccount = linked).connectionLabel(),
                style = MaterialTheme.typography.labelLarge,
                color =
                    if (draftConfigured) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
            )

            if (info.signInMethods.size > 1) {
                Text("Sign-in method", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    info.signInMethods.forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(m.label) },
                        )
                    }
                }
            }

            when (method) {
                LlmSignInMethod.API_KEY -> {
                    if (info.requiresApiKey) {
                        Text(info.apiKeyHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API key / token") },
                            placeholder = { Text(info.apiKeyPlaceholder) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(autoCorrect = false),
                        )
                    }
                }
                LlmSignInMethod.GOOGLE,
                LlmSignInMethod.EMAIL,
                LlmSignInMethod.PHONE,
                -> {
                    val dest = LlmBrowserAuth.destinationFor(info, method)
                    val instructions = dest?.instructions ?: "Sign in on the provider website, then paste your API key below."
                    Text(instructions, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    browserHint?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    OutlinedButton(
                        onClick = {
                            if (dest != null) {
                                LlmBrowserAuth.openSignIn(context, dest)
                                browserHint = "After you sign in, return here and paste your API key."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = dest != null,
                    ) {
                        Text("Open ${method.label} in browser")
                    }
                    if (method == LlmSignInMethod.GOOGLE && webClientId.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    vm.signInWithGoogle(webClientId) { email ->
                                        linked = email
                                        browserHint = "Signed in as $email — add your API key below."
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Sign in with Google on device")
                        }
                    }
                    if (info.requiresApiKey) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("Paste API key / token") },
                            placeholder = { Text(info.apiKeyPlaceholder) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        )
                    }
                    OutlinedTextField(
                        value = linked,
                        onValueChange = { linked = it },
                        label = { Text("Account email (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }

            if (profile.kind != LlmProviderKind.HUGGING_FACE && profile.kind != LlmProviderKind.OLLAMA) {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model id") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            if (profile.kind == LlmProviderKind.OLLAMA || !profile.isBuiltIn) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            if (profile.kind == LlmProviderKind.HUGGING_FACE) {
                HuggingFaceModelPicker(
                    profile = profile.copy(apiKey = apiKey, model = model, baseUrl = HuggingFaceDefaults.ROUTER_BASE_URL),
                    onSave = { updated ->
                        apiKey = updated.apiKey
                        model = updated.model
                    },
                    onSearch = vm::searchHuggingFaceModels,
                    pickOnly = true,
                )
            }

            Button(
                onClick = {
                    vm.saveLlmConnection(
                        profile.copy(
                            apiKey = apiKey.trim(),
                            linkedAccount = linked.trim(),
                            model = model.trim(),
                            baseUrl = baseUrl.trim().ifBlank { profile.baseUrl },
                        ),
                        activate = true,
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = profile.kind == LlmProviderKind.OLLAMA || apiKey.isNotBlank() || !info.requiresApiKey,
            ) {
                Text("Save & use this provider")
            }
            if (profile.isConfigured()) {
                OutlinedButton(
                    onClick = {
                        vm.disconnectLlmProfile(profile.id)
                        apiKey = ""
                        linked = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Disconnect")
                }
            }
        }
    }
}
