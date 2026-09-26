package com.skillmcp.mentor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.ComposerBar
import com.skillmcp.mentor.ui.components.MessageBubble
import com.skillmcp.mentor.ui.components.SuggestionChipRow
import kotlinx.coroutines.launch

private val starters =
    listOf(
        "Help me plan my week",
        "Explain a topic simply",
        "Draft a professional email",
        "Walk through a problem step by step",
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val extraItems =
        (if (state.isSending && state.streamPreview.isNotBlank()) 1 else if (state.isSending) 1 else 0)
    LaunchedEffect(state.messages.size, extraItems, state.streamPreview) {
        val last = state.messages.size + extraItems - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(
                    "Conversations",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                )
                state.conversations.forEach { chat ->
                    NavigationDrawerItem(
                        label = { Text(chat.name) },
                        selected = chat.id == state.activeConversationId,
                        onClick = {
                            vm.selectConversation(chat.id)
                            scope.launch { drawer.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
                NavigationDrawerItem(
                    label = { Text("New conversation") },
                    selected = false,
                    icon = { Icon(Icons.Default.Add, null) },
                    onClick = {
                        vm.newConversation()
                        scope.launch { drawer.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    shape = MaterialTheme.shapes.medium,
                )
            }
        },
    ) {
        AppBackground {
            Column(Modifier.fillMaxSize()) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text("Universal AI", fontWeight = FontWeight.Bold)
                            state.activeLlmProfile?.let {
                                Text(
                                    it.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawer.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Conversations")
                        }
                    },
                    colors =
                        TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        ),
                )

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (state.messages.isEmpty() && !state.isSending) {
                        item("hero") {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 32.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "What can I help with?",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Your assistant for learning, planning, creativity, and everyday questions.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(24.dp))
                                SuggestionChipRow(
                                    labels = starters.map { prompt -> prompt to { vm.applySuggestion(prompt) } },
                                )
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
                            Text(
                                "● ● ●",
                                modifier = Modifier.padding(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = state.suggestions.isNotEmpty()) {
                    SuggestionChipRow(
                        labels = state.suggestions.map { it.label to { vm.applySuggestion(it.prompt) } },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                state.status?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}
