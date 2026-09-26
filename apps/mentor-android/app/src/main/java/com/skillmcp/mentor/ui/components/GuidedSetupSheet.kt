package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.mentor.PopularUseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedSetupSheet(
    visible: Boolean,
    useCases: List<PopularUseCase>,
    onConnectOpenAi: () -> Unit,
    onSendTestMessage: () -> Unit,
    onPickUseCase: (PopularUseCase) -> Unit,
    onFinish: () -> Unit,
) {
    if (!visible) return
    var step by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onFinish, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Setup (${step + 1}/4)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (step) {
                0 -> {
                    Text("Welcome to Universal AI", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "We'll connect a model, try one message, and pick a starter workflow. You can change everything later in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Start") }
                }
                1 -> {
                    Text("Choose a provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "OpenAI is a common starting point. You'll paste an API key or sign in on the provider website.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            onConnectOpenAi()
                            step = 2
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Connect OpenAI")
                    }
                    OutlinedButton(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth()) {
                        Text("Skip for now")
                    }
                }
                2 -> {
                    Text("Send a test message", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "We'll open Chat with a short hello so you can confirm the model responds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            onSendTestMessage()
                            step = 3
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open Chat with test prompt")
                    }
                }
                else -> {
                    Text("Pick a starter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    useCases.take(4).forEach { useCase ->
                        OutlinedButton(
                            onClick = {
                                onPickUseCase(useCase)
                                onFinish()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(useCase.title)
                        }
                    }
                    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Finish setup") }
                }
            }
        }
    }
}
