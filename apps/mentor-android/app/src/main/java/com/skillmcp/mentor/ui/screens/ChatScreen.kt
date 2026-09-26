package com.skillmcp.mentor.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.skillmcp.mentor.llm.ModelPreset
import com.skillmcp.mentor.ui.components.PromptLibrarySheet
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.mentor.UiConversation
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.ComposerBar
import com.skillmcp.mentor.ui.components.MessageBubble
import com.skillmcp.mentor.mentor.ScreenSuggestions
import com.skillmcp.mentor.mentor.SuggestionScreen
import com.skillmcp.mentor.ui.components.SuggestionChipRow
import com.skillmcp.mentor.ui.components.TabSuggestions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showPrompts by remember { mutableStateOf(false) }
    var drawerQuery by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<UiConversation?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    val filteredChats =
        state.conversations.filter { it.name.contains(drawerQuery, ignoreCase = true) }

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
                OutlinedTextField(
                    value = drawerQuery,
                    onValueChange = { drawerQuery = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                    placeholder = { Text("Search chats") },
                    singleLine = true,
                )
                filteredChats.forEach { chat ->
                    ConversationDrawerRow(
                        chat = chat,
                        selected = chat.id == state.activeConversationId,
                        onOpen = {
                            vm.selectConversation(chat.id)
                            scope.launch { drawer.close() }
                        },
                        onRename = {
                            renameTarget = chat
                            renameDraft = chat.name
                        },
                        onTogglePin = { vm.pinConversation(chat.id, !chat.pinned) },
                        onDelete = { vm.deleteConversation(chat.id) },
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
            renameTarget?.let { chat ->
                AlertDialog(
                    onDismissRequest = { renameTarget = null },
                    title = { Text("Rename conversation") },
                    text = {
                        OutlinedTextField(
                            value = renameDraft,
                            onValueChange = { renameDraft = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                vm.renameConversation(chat.id, renameDraft)
                                renameTarget = null
                            },
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { renameTarget = null }) {
                            Text("Cancel")
                        }
                    },
                )
            }
            if (showPrompts) {
                PromptLibrarySheet(
                    prompts = state.savedPrompts,
                    onDismiss = { showPrompts = false },
                    onSelect = vm::applySuggestion,
                    onSave = vm::savePrompt,
                    onDelete = vm::deletePrompt,
                )
            }
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
                    actions = {
                        IconButton(onClick = { showPrompts = true }) {
                            Icon(Icons.Outlined.Lightbulb, contentDescription = "Prompt library")
                        }
                    },
                    colors =
                        TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ModelPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = state.prefs.modelPreset == preset,
                            onClick = { vm.setModelPreset(preset) },
                            label = { Text(preset.label) },
                        )
                    }
                }

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
                                Spacer(Modifier.height(16.dp))
                                TabSuggestions(
                                    title = "Try these",
                                    suggestions = ScreenSuggestions.forScreen(SuggestionScreen.CHAT),
                                    onSelect = vm::applySuggestion,
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

                if (state.messages.isNotEmpty() && state.suggestions.isNotEmpty()) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            "Suggestions",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        SuggestionChipRow(
                            labels = state.suggestions.take(7).map { it.label to { vm.applySuggestion(it.prompt) } },
                        )
                        if (state.suggestions.size > 7) {
                            SuggestionChipRow(
                                labels = state.suggestions.drop(7).map { it.label to { vm.applySuggestion(it.prompt) } },
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
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

@Composable
private fun ConversationDrawerRow(
    chat: UiConversation,
    selected: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NavigationDrawerItem(
            label = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (chat.pinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Text(chat.name, maxLines = 1)
                }
            },
            selected = selected,
            onClick = onOpen,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
        )
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Conversation options")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    menuOpen = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text(if (chat.pinned) "Unpin" else "Pin") },
                leadingIcon = {
                    Icon(
                        if (chat.pinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                        contentDescription = null,
                    )
                },
                onClick = {
                    menuOpen = false
                    onTogglePin()
                },
            )
            if (chat.id != "default") {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}
