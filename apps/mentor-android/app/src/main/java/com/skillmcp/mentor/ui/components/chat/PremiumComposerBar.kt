package com.skillmcp.mentor.ui.components.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.R
import com.skillmcp.mentor.ui.components.motion.pressableScale
import com.skillmcp.mentor.ui.motion.CalmMotion
import com.skillmcp.mentor.ui.motion.rememberReduceMotion
import com.skillmcp.mentor.ui.theme.ComposerShape
import com.skillmcp.mentor.ui.theme.SendButtonShape

/** What the trailing composer button currently does. */
enum class ComposerAction { VOICE, SEND, STOP }

fun composerAction(hasText: Boolean, isSending: Boolean): ComposerAction =
    when {
        isSending -> ComposerAction.STOP
        hasText -> ComposerAction.SEND
        else -> ComposerAction.VOICE
    }

/**
 * Floating composer. The trailing button morphs voice → send (when there is text) → stop (while a reply
 * streams), like the ChatGPT / Gemini apps. The attach button hides while a reply is generating.
 */
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
    onStop: () -> Unit = {},
    editing: Boolean = false,
    onCancelEdit: () -> Unit = {},
) {
    val reduceMotion = rememberReduceMotion()
    val action = composerAction(draft.isNotBlank(), isSending)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ComposerShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
    ) {
        Column {
            AnimatedVisibility(editing) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 4.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.chat_editing_banner), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(
                            stringResource(R.string.chat_editing_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onCancelEdit) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.chat_cancel_edit))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                if (onAttach != null) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isSending,
                            enter = if (reduceMotion) EnterTransition.None else fadeIn() + scaleIn(),
                            exit = if (reduceMotion) ExitTransition.None else fadeOut() + scaleOut(),
                        ) {
                            IconButton(onClick = onAttach, modifier = Modifier.pressableScale()) {
                                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.composer_attach_label))
                            }
                        }
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = draft,
                    onValueChange = onDraftChange,
                    placeholder = { Text(stringResource(R.string.composer_placeholder)) },
                    minLines = 1,
                    maxLines = 6,
                    shape = ComposerShape,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                )
                AnimatedContent(
                    targetState = action,
                    transitionSpec = {
                        if (reduceMotion) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            (fadeIn(CalmMotion.fastTween(false)) + scaleIn(CalmMotion.gentle(), initialScale = 0.6f)) togetherWith
                                (fadeOut(CalmMotion.fastTween(false)) + scaleOut(targetScale = 0.6f))
                        }
                    },
                    label = "composerAction",
                ) { current ->
                    when (current) {
                        ComposerAction.SEND ->
                            FilledIconButton(
                                onClick = onSend,
                                modifier = Modifier.size(48.dp).pressableScale(),
                                shape = SendButtonShape,
                                colors =
                                    IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = stringResource(R.string.composer_send))
                            }
                        ComposerAction.STOP ->
                            FilledIconButton(
                                onClick = onStop,
                                modifier = Modifier.size(48.dp).pressableScale(),
                                shape = SendButtonShape,
                                colors =
                                    IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.onSurface,
                                        contentColor = MaterialTheme.colorScheme.surface,
                                    ),
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = stringResource(R.string.composer_stop))
                            }
                        ComposerAction.VOICE ->
                            FilledTonalIconButton(
                                onClick = onMic,
                                enabled = !isListening,
                                modifier = Modifier.size(48.dp).pressableScale(),
                                shape = SendButtonShape,
                            ) {
                                if (isListening) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                                } else {
                                    Icon(Icons.Rounded.Mic, contentDescription = stringResource(R.string.composer_voice))
                                }
                            }
                    }
                }
            }
        }
    }
}
