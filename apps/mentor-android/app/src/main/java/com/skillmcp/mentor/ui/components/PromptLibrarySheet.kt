package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.data.db.SavedPromptEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptLibrarySheet(
    prompts: List<SavedPromptEntity>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onSave: (title: String, body: String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Prompt library", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            items(prompts, key = { it.id }) { prompt ->
                GlassCard {
                    Text(prompt.title, fontWeight = FontWeight.SemiBold)
                    Text(prompt.body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
                    Button(onClick = { onSelect(prompt.body); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Use prompt")
                    }
                    Button(onClick = { onDelete(prompt.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Delete")
                    }
                }
            }
            item {
                GlassCard {
                    Text("Save current idea", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        body,
                        { body = it },
                        label = { Text("Prompt text") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    Button(
                        onClick = {
                            onSave(title, body)
                            title = ""
                            body = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = body.isNotBlank(),
                    ) {
                        Text("Save to library")
                    }
                }
            }
        }
    }
}
