package com.skillmcp.mentor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.ComposerBar
import com.skillmcp.mentor.ui.components.MessageBubble
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private val starters =
    listOf(
        "Help me plan my week",
        "Explain quantum computing simply",
        "Draft a professional email",
        "Debug my code step by step",
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.isSending) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Chats",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(20.dp),
                )
                state.conversations.forEach { chat ->
                    NavigationDrawerItem(
                        label = { Text(chat.name) },
                        selected = chat.id == state.activeConversationId,
                        onClick = {
                            vm.selectConversation(chat.id)
                            scope.launch { drawer.close() }
                        },
                    )
                }
                NavigationDrawerItem(
                    label = { Text("New chat") },
                    selected = false,
                    icon = { Icon(Icons.Default.Add, null) },
                    onClick = {
                        vm.newConversation()
                        scope.launch { drawer.close() }
                    },
                )
            }
        },
    ) {
        AppBackground {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Universal AI", fontWeight = FontWeight.SemiBold)
                            state.activeLlmProfile?.let {
                                Text(
                                    "${it.name} · ${it.model}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawer.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Chats")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                )

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.messages.isEmpty()) {
                        item {
                            Column(Modifier.padding(vertical = 24.dp)) {
                                Text(
                                    "What can I help with?",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Ask about anything — learning, work, creativity, or code.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(20.dp))
                                starters.forEach { prompt ->
                                    AssistChip(
                                        onClick = { vm.applySuggestion(prompt) },
                                        label = { Text(prompt) },
                                        modifier = Modifier.padding(bottom = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                    items(state.messages, key = { it.id }) { message ->
                        if (message.content.isNotBlank()) {
                            MessageBubble(content = message.content, isUser = message.role == "user")
                        }
                    }
                    if (state.isSending && state.streamPreview.isNotBlank()) {
                        item("stream-preview") {
                            MessageBubble(content = state.streamPreview, isUser = false)
                        }
                    } else if (state.isSending) {
                        item("stream-typing") {
                            Text("…", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(12.dp))
                        }
                    }
                }

                AnimatedVisibility(visible = state.suggestions.isNotEmpty()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.suggestions.forEach { s ->
                            AssistChip(onClick = { vm.applySuggestion(s.prompt) }, label = { Text(s.label) })
                        }
                    }
                }

                state.status?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                ComposerBar(
                    draft = state.draft,
                    onDraftChange = vm::onDraftChange,
                    onSend = vm::sendMessage,
                    onMic = vm::toggleListen,
                    isSending = state.isSending,
                    isListening = state.isListening,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}
