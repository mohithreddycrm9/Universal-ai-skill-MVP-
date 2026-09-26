package com.skillmcp.mentor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.skillmcp.mentor.llm.ModelPreset
import com.skillmcp.mentor.llm.connectSignInBlurb
import com.skillmcp.mentor.llm.isConfigured
import com.skillmcp.mentor.ui.components.PromptLibrarySheet
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
import com.skillmcp.mentor.ui.components.PopularUseCaseCard
import com.skillmcp.mentor.ui.components.TabSuggestions
import com.skillmcp.mentor.policy.SpendBlockReason
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
    val context = LocalContext.current
    val micPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) vm.toggleListen()
        }
    val onMic: () -> Unit = {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> vm.toggleListen()
            else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val extraItems =
        (if (state.isSending && state.streamPreview.isNotBlank()) 1 else if (state.isSending) 1 else 0)
    LaunchedEffect(state.messages.size, extraItems, state.streamPreview) {
        val last = state.messages.size + extraItems - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    val raisePreview by vm.spendRaisePreview.collectAsState()
    raisePreview?.let { (daily, weekly) ->
        AlertDialog(
            onDismissRequest = vm::dismissSpendRaisePreview,
            title = { Text("Raise spend limits?") },
            text = {
                Text(
                    "Estimated daily cap → $${"%.2f".format(daily)}\n" +
                        "Estimated weekly cap → $${"%.2f".format(weekly)}\n\n" +
                        "Amounts are estimates from Usage, not your provider bill.",
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = vm::confirmRaiseSpendLimits) { Text("Confirm") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = vm::dismissSpendRaisePreview) { Text("Cancel") }
            },
        )
    }

    state.pendingShare?.let { share ->
        AlertDialog(
            onDismissRequest = vm::declineSharedContent,
            title = { Text("Send to your AI provider?") },
            text = {
                Text(
                    "Shared content will be sent to ${state.activeLlmProfile?.name ?: "your connected model"} when you tap Send. " +
                        "The provider may process URLs and text on their servers.",
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = vm::acceptSharedContent) {
                    Text("Continue")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = vm::declineSharedContent) {
                    Text("Cancel")
                }
            },
        )
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
                        IconButton(
                            onClick = { scope.launch { drawer.open() } },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Conversations menu")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showPrompts = true },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(Icons.Outlined.Lightbulb, contentDescription = "Open prompt library")
                        }
                    },
                    colors =
                        TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        ),
                )
                state.activeLlmProfile?.takeIf { !it.isConfigured() }?.let { profile ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Connect ${profile.name} to start chatting",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                connectSignInBlurb(profile.kind),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.openConnectLlm(profile.id) }) {
                                    Text("Connect")
                                }
                                androidx.compose.material3.TextButton(
                                    onClick = { vm.requestOpenTab("models") },
                                ) {
                                    Text("All providers")
                                }
                            }
                        }
                    }
                }
                state.spendGuard.warningMessage?.let { warning ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Text(
                            warning,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                if (!state.spendGuard.allowed) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                state.spendGuard.message ?: "Estimated spend limit reached.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = vm::previewRaiseSpendLimits) { Text("Raise limit") }
                                if (state.spendGuard.blockReason == SpendBlockReason.DAILY) {
                                    androidx.compose.material3.TextButton(onClick = vm::dismissSpendBlockMessage) {
                                        Text("Wait until tomorrow")
                                    }
                                } else {
                                    androidx.compose.material3.TextButton(onClick = { vm.requestOpenTab("usage") }) {
                                        Text("Adjust on Usage")
                                    }
                                }
                            }
                        }
                    }
                }
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
                                    "New conversation",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Ask a question or pick a starter below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    "Starters",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    items(state.popularUseCases, key = { it.id }) { useCase ->
                                        PopularUseCaseCard(
                                            useCase = useCase,
                                            onClick = { vm.startPopularUseCase(useCase) },
                                        )
                                    }
                                }
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

                state.status?.let { msg ->
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        Text(
                            msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        state.statusDetails?.let { details ->
                            var show by remember { mutableStateOf(false) }
                            androidx.compose.material3.TextButton(onClick = { show = !show }) {
                                Text(if (show) "Hide details" else "Details")
                            }
                            if (show) {
                                Text(
                                    details,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                ComposerBar(
                    draft = state.draft,
                    onDraftChange = vm::onDraftChange,
                    onSend = vm::sendMessage,
                    onMic = onMic,
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
