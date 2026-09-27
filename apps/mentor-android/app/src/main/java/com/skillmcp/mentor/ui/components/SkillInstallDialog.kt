package com.skillmcp.mentor.ui.components

import com.skillmcp.mentor.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.skillmcp.mentor.ui.SkillInstallRequest

@Composable
fun SkillInstallDialog(
    request: SkillInstallRequest?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (request == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Install “${request.title}”?") },
        text = {
            Text(
                "Source: ${request.sourceUrl}\n\n" +
                    "Trust: ${request.trustTier}. " +
                    if (request.needsNetwork) {
                        "This pack may instruct the model to use network tools when enabled."
                    } else {
                        "Offline instructions only."
                    },
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_install)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
