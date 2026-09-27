package com.skillmcp.mentor.ui.components.chat

import com.skillmcp.mentor.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.skillmcp.mentor.ui.components.motion.pressableScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.ui.theme.ComposerShape
import com.skillmcp.mentor.ui.theme.SendButtonShape

@Composable
fun PremiumComposerBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit,
    onAttach: (() -> Unit)?,
    isSending: Boolean,
    isListening: Boolean,
    modifier: Modifier = Modifier,
) {
    val hasText = draft.isNotBlank()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ComposerShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        tonalElevation = 3.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (onAttach != null) {
                IconButton(onClick = onAttach, modifier = Modifier.size(48.dp).pressableScale()) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.composer_attach))
                }
            }
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text(stringResource(R.string.composer_placeholder)) },
                minLines = 1,
                maxLines = 5,
                shape = ComposerShape,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
            )
            AnimatedContent(
                targetState = hasText,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "sendMic",
            ) { showSend ->
                if (showSend) {
                    FilledIconButton(
                        onClick = onSend,
                        enabled = !isSending,
                        modifier =
                            Modifier
                                .size(48.dp)
                                .pressableScale()
                                .semantics {
                                    contentDescription = if (isSending) "Sending message" else "Send message"
                                },
                        shape = SendButtonShape,
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.composer_send))
                        }
                    }
                } else {
                    FilledIconButton(
                        onClick = onMic,
                        enabled = !isListening && !isSending,
                        modifier = Modifier.size(48.dp).pressableScale(),
                        shape = SendButtonShape,
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ),
                    ) {
                        if (isListening) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        } else {
                            Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.composer_voice))
                        }
                    }
                }
            }
        }
    }
}
