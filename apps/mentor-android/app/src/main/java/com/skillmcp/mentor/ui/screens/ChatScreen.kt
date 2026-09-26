package com.skillmcp.mentor.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.ui.MentorViewModel

@Composable
fun ChatScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Code Mentor", style = MaterialTheme.typography.titleLarge)
        if (state.prefs.buildGoal.isNotBlank()) {
            Text("Goal: ${state.prefs.buildGoal}", style = MaterialTheme.typography.bodyMedium)
        }

        if (state.suggestions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.suggestions.forEach { suggestion ->
                    AssistChip(
                        onClick = { vm.applySuggestion(suggestion.prompt) },
                        label = { Text(suggestion.label) },
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages) { message ->
                val isUser = message.role == "user"
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            if (isUser) "You" else "Mentor",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(message.content)
                    }
                }
            }
        }

        state.status?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = state.draft,
                onValueChange = vm::onDraftChange,
                placeholder = { Text("Ask your mentor…") },
                minLines = 1,
                maxLines = 4,
            )
            FilledIconButton(
                onClick = vm::toggleListen,
                enabled = !state.isListening,
            ) {
                if (state.isListening) {
                    CircularProgressIndicator()
                } else {
                    Icon(Icons.Default.Mic, contentDescription = "Speak")
                }
            }
            FilledIconButton(
                onClick = vm::sendMessage,
                enabled = !state.isSending && state.draft.isNotBlank(),
            ) {
                if (state.isSending) {
                    CircularProgressIndicator()
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}
