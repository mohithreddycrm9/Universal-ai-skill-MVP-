package com.skillmcp.mentor.ui.screens

import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.skillmcp.mentor.llm.ModelPreset
import com.skillmcp.mentor.ui.components.PromptLibrarySheet
import androidx.compose.ui.Alignment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import com.skillmcp.mentor.R
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.mentor.UiConversation
import com.skillmcp.mentor.ui.MentorViewModel
import com.skillmcp.mentor.ui.components.AppBackground
import com.skillmcp.mentor.ui.components.chat.ChatEmptyState
import com.skillmcp.mentor.ui.components.chat.ChatMessageContent
import com.skillmcp.mentor.ui.components.chat.PremiumComposerBar
import com.skillmcp.mentor.ui.components.chat.ChatHistoryDrawer
import com.skillmcp.mentor.ui.components.chat.ThinkingShimmerLine
import com.skillmcp.mentor.ui.components.chat.groupConversationsForDrawer
import com.skillmcp.mentor.ui.motion.CalmMotion
import com.skillmcp.mentor.ui.motion.rememberReduceMotion
import com.skillmcp.mentor.ui.util.performLightTap
import com.skillmcp.mentor.ui.util.performSendHaptic
import com.skillmcp.mentor.ui.util.rememberHapticView
import com.skillmcp.mentor.ui.components.chat.VoiceModeOverlay
import com.skillmcp.mentor.ui.components.chat.popularUseCasesToStarters
import com.skillmcp.mentor.ui.theme.MentorDimens
import com.skillmcp.mentor.mentor.ScreenSuggestions
import com.skillmcp.mentor.mentor.SuggestionScreen
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ChatScreen(vm: MentorViewModel) {
    val state by vm.uiState.collectAsState()
    val searchResults by vm.drawerSearchResults.collectAsState()
    val pendingImageUri by vm.pendingImagePreviewUri.collectAsState()
    val pendingPdf by vm.pendingPdfExtract.collectAsState()
    ChatScreenContent(
        state = state,
        searchResults = searchResults,
        pendingImageUri = pendingImageUri,
        pendingPdf = pendingPdf,
        vm = vm,
    )
}

/** Stateless chat UI; snapshot tests pass a no-op PreviewChatActions (src/test). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ChatScreenContent(
    state: com.skillmcp.mentor.ui.MentorUiState,
    searchResults: List<UiConversation>?,
    pendingImageUri: android.net.Uri?,
    pendingPdf: String?,
    vm: com.skillmcp.mentor.ui.chat.ChatScreenActions,
) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showPrompts by remember { mutableStateOf(false) }
    var drawerQuery by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<UiConversation?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var tagTarget by remember { mutableStateOf<UiConversation?>(null) }
    var tagDraft by remember { mutableStateOf("") }
    val context = LocalContext.current
    LaunchedEffect(drawerQuery) { vm.searchChats(drawerQuery) }
    // Title/tag matches are listed as chats; chats that only match inside messages get a snippet row.
    val filteredChats =
        if (drawerQuery.isBlank()) {
            state.conversations
        } else {
            state.conversations.filter {
                it.name.contains(drawerQuery, ignoreCase = true) || it.folderTag.contains(drawerQuery, ignoreCase = true)
            }
        }
    val filteredIds = filteredChats.mapTo(HashSet()) { it.id }
    val messageHits =
        if (drawerQuery.isBlank() || searchResults == null) emptyList() else state.messageSearchHits.filter { it.conversationId !in filteredIds }
    val drawerSections = groupConversationsForDrawer(filteredChats)
    var deleteTarget by remember { mutableStateOf<UiConversation?>(null) }
    var chatMenuOpen by remember { mutableStateOf(false) }
    var showModelSheet by remember { mutableStateOf(false) }
    val chatTitle =
        state.conversations.find { it.id == state.activeConversationId }?.name
            ?: stringResource(R.string.nav_chat)
    val hapticView = rememberHapticView()
    val reduceMotion = rememberReduceMotion()
    // Per-chat scroll: a fresh list state per conversation, seeded from the persisted position.
    val conversationKey = state.activeConversationId
    val savedScroll = remember(conversationKey) { vm.chatScrollFor(conversationKey) }
    val listState = remember(conversationKey) { LazyListState() }
    val scrollController =
        com.skillmcp.mentor.ui.chat.rememberChatScrollController(
            listState,
            initialFollowBottom = savedScroll?.atBottom ?: true,
        )
    // Messages of the previous chat can still be on screen for a frame after switching; wait for new ones.
    val staleFirstId = remember(conversationKey) { state.messages.firstOrNull()?.id }
    var scrollRestored by remember(conversationKey) { mutableStateOf(false) }
    val firstId = state.messages.firstOrNull()?.id
    LaunchedEffect(conversationKey, firstId) {
        val saved = savedScroll
        if (!scrollRestored && firstId != null && (staleFirstId == null || firstId != staleFirstId)) {
            scrollRestored = true
            if (saved != null && !saved.atBottom) listState.scrollToItem(saved.index, saved.offset)
        }
    }
    LaunchedEffect(conversationKey, listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling && listState.layoutInfo.totalItemsCount > 0) {
                    vm.saveChatScroll(
                        conversationKey,
                        com.skillmcp.mentor.data.ChatScrollPosition(
                            index = listState.firstVisibleItemIndex,
                            offset = listState.firstVisibleItemScrollOffset,
                            atBottom = scrollController.followBottom,
                        ),
                    )
                }
            }
    }
    var prevSending by remember { mutableStateOf(false) }
    LaunchedEffect(state.isSending) {
        if (prevSending && !state.isSending) {
            hapticView.performLightTap()
        }
        prevSending = state.isSending
    }
    com.skillmcp.mentor.ui.chat.ChatAutoScrollEffect(scrollController, reduceMotion) {
        Triple(listState.layoutInfo.totalItemsCount, state.streamPreview.length, state.showReplySlot)
    }
    val attachLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                val mime = context.contentResolver.getType(uri)
                vm.attachFromUri(uri, mime)
            }
        }
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


    state.pendingShare?.let { share ->
        AlertDialog(
            onDismissRequest = vm::declineSharedContent,
            title = { Text(stringResource(R.string.chat_share_consent_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.chat_share_consent_body,
                        state.chatLlmProfile?.name ?: stringResource(R.string.chat_share_consent_model_fallback),
                    ),
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = vm::acceptSharedContent) {
                    Text(stringResource(R.string.action_continue))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = vm::declineSharedContent) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ChatHistoryDrawer(
                query = drawerQuery,
                onQueryChange = { drawerQuery = it },
                sections = drawerSections,
                activeId = state.activeConversationId,
                onNewChat = {
                    vm.newConversation()
                    scope.launch { drawer.close() }
                },
                onOpenChat = { chat ->
                    vm.selectConversation(chat.id)
                    scope.launch { drawer.close() }
                },
                conversationRow = { chat, selected, onOpen ->
                    ConversationDrawerRow(
                        chat = chat,
                        selected = selected,
                        onOpen = onOpen,
                        onRename = {
                            renameTarget = chat
                            renameDraft = chat.name
                        },
                        onTogglePin = { vm.pinConversation(chat.id, !chat.pinned) },
                        onDelete = { deleteTarget = chat },
                        onSetTag = {
                            tagTarget = chat
                            tagDraft = chat.folderTag
                        },
                        onShareMarkdown = { vm.shareChatMarkdown(chat.id) },
                        onSharePdf = { vm.shareChatPdf(chat.id) },
                    )
                },
                messageHits = messageHits,
                onOpenHit = { hit ->
                    vm.selectConversation(hit.conversationId)
                    scope.launch { drawer.close() }
                },
                // The tab bar is hidden inside a conversation; the drawer keeps the other tabs one tap away.
                footer = {
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    androidx.compose.material3.NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_discover)) },
                        selected = false,
                        icon = { Icon(Icons.Rounded.Explore, contentDescription = null) },
                        onClick = {
                            scope.launch { drawer.close() }
                            vm.requestOpenTab("discover")
                        },
                        modifier = Modifier.padding(androidx.compose.material3.NavigationDrawerItemDefaults.ItemPadding),
                    )
                    androidx.compose.material3.NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_settings)) },
                        selected = false,
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                        onClick = {
                            scope.launch { drawer.close() }
                            vm.requestOpenTab("settings")
                        },
                        modifier = Modifier.padding(androidx.compose.material3.NavigationDrawerItemDefaults.ItemPadding),
                    )
                },
            )
        },
    ) {
        AppBackground {
            deleteTarget?.let { chat ->
                AlertDialog(
                    onDismissRequest = { deleteTarget = null },
                    icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                    title = { Text(stringResource(R.string.chat_delete_title)) },
                    text = { Text(stringResource(R.string.chat_delete_body, chat.name)) },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                vm.deleteConversation(chat.id)
                                deleteTarget = null
                            },
                        ) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.action_cancel)) }
                    },
                )
            }
            tagTarget?.let { chat ->
                AlertDialog(
                    onDismissRequest = { tagTarget = null },
                    title = { Text(stringResource(R.string.chat_tag_title)) },
                    text = {
                        OutlinedTextField(
                            value = tagDraft,
                            onValueChange = { tagDraft = it },
                            label = { Text(stringResource(R.string.chat_tag_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                vm.setConversationTag(chat.id, tagDraft)
                                tagTarget = null
                            },
                        ) {
                            Text(stringResource(R.string.action_save))
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { tagTarget = null }) { Text(stringResource(R.string.action_cancel)) }
                    },
                )
            }
            renameTarget?.let { chat ->
                AlertDialog(
                    onDismissRequest = { renameTarget = null },
                    title = { Text(stringResource(R.string.chat_rename_title)) },
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
                            Text(stringResource(R.string.action_save))
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { renameTarget = null }) {
                            Text(stringResource(R.string.action_cancel))
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
            Column(Modifier.fillMaxSize().imePadding()) {
                if (showModelSheet) {
                    com.skillmcp.mentor.ui.components.chat.ModelSwitcherSheet(
                        profiles = state.llmProfiles,
                        currentId = state.chatLlmProfile?.id,
                        onPick = {
                            showModelSheet = false
                            vm.switchChatModel(it.id)
                        },
                        onConnect = {
                            showModelSheet = false
                            vm.connectModel(it.id)
                        },
                        onManage = {
                            showModelSheet = false
                            vm.requestOpenTab("models")
                        },
                        onDismiss = { showModelSheet = false },
                    )
                }
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                chatTitle,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            com.skillmcp.mentor.ui.components.chat.ModelChip(
                                profile = state.chatLlmProfile,
                                onClick = { showModelSheet = true },
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawer.open() } }) {
                            Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.chat_open_chats))
                        }
                    },
                    actions = {
                        IconButton(onClick = vm::newConversation) {
                            Icon(Icons.Rounded.EditNote, contentDescription = stringResource(R.string.chat_new_chat))
                        }
                        if (state.messages.isNotEmpty()) {
                            Box {
                                IconButton(onClick = { chatMenuOpen = true }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.chat_more_options))
                                }
                                val active = state.conversations.find { it.id == state.activeConversationId }
                                DropdownMenu(expanded = chatMenuOpen, onDismissRequest = { chatMenuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.chat_share_text)) },
                                        leadingIcon = { Icon(Icons.Rounded.Share, null) },
                                        onClick = {
                                            chatMenuOpen = false
                                            vm.shareChatText(state.activeConversationId)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.chat_share_markdown)) },
                                        leadingIcon = { Icon(Icons.Rounded.Description, null) },
                                        onClick = {
                                            chatMenuOpen = false
                                            vm.shareChatMarkdown(state.activeConversationId)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.chat_share_pdf)) },
                                        leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, null) },
                                        onClick = {
                                            chatMenuOpen = false
                                            vm.shareChatPdf(state.activeConversationId)
                                        },
                                    )
                                    if (active != null) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_rename)) },
                                            leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null) },
                                            onClick = {
                                                chatMenuOpen = false
                                                renameTarget = active
                                                renameDraft = active.name
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(if (active.pinned) R.string.chat_unpin else R.string.chat_pin)) },
                                            leadingIcon = { Icon(Icons.Rounded.PushPin, null) },
                                            onClick = {
                                                chatMenuOpen = false
                                                vm.pinConversation(active.id, !active.pinned)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        ),
                )
                if (!state.messageAllowance.allowed) {
                    val hidden =
                        com.skillmcp.mentor.policy.SpendPolicy.shouldHideDailyBlockUi(
                            state.messageAllowance,
                            state.prefs.spendDailyBlockDismissedUntilMs,
                        )
                    if (!hidden) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = MentorDimens.ScreenHorizontal),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                state.messageAllowance.message ?: stringResource(R.string.chat_limit_reached),
                                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            // Hides the reminder for the same rolling 24h window the limit uses; sending stays blocked.
                            if (state.messageAllowance.blockReason ==
                                com.skillmcp.mentor.policy.AllowanceBlockReason.DAILY
                            ) {
                                androidx.compose.material3.TextButton(onClick = vm::dismissSpendBlockMessage) {
                                    Text(stringResource(R.string.chat_limit_hide_24h))
                                }
                            }
                        }
                    }
                } else {
                    state.messageAllowance.warningMessage?.let { warning ->
                        Text(
                            warning,
                            modifier = Modifier.padding(horizontal = MentorDimens.ScreenHorizontal, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    // testTag is exposed as a resource id for the Macrobenchmark / Baseline Profile journeys.
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .semantics { testTagsAsResourceId = true }
                            .testTag("chat_list"),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = MentorDimens.ScreenHorizontal, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (state.messages.isEmpty() && !state.isSending) {
                        item("hero") {
                            ChatEmptyState(
                                displayName = state.prefs.displayName,
                                starters =
                                    popularUseCasesToStarters(state.popularUseCases, vm::startPopularUseCase),
                            )
                        }
                    }
                    val chatModelLabel = state.chatLlmProfile?.let { com.skillmcp.mentor.llm.ConversationContext.modelLabel(it) }
                    val lastId = state.messages.lastOrNull { it.role == "user" || it.role == "assistant" }?.id
                    items(state.messages, key = { it.id }) { message ->
                        val isUser = message.role == "user"
                        if (message.role == com.skillmcp.mentor.llm.ROLE_MODEL_SWITCH) {
                            com.skillmcp.mentor.ui.components.chat.ModelSwitchDivider(
                                modelLabel = message.content,
                                modifier = Modifier.calmItem(this, reduceMotion),
                            )
                            return@items
                        }
                        val regenerating = message.id == state.regeneratingMessageId
                        if (regenerating) {
                            // Regenerate streams into the same row (same key), replacing the reply in place.
                            androidx.compose.animation.Crossfade(
                                modifier = Modifier.calmItem(this, reduceMotion),
                                targetState = state.streamPreview.isBlank(),
                                animationSpec = CalmMotion.fastTween(reduceMotion),
                                label = "regenerate",
                            ) { thinking ->
                                if (thinking) {
                                    Column(Modifier.padding(top = 12.dp)) {
                                        com.skillmcp.mentor.ui.components.chat.AssistantHeader(chatModelLabel)
                                        ThinkingShimmerLine()
                                    }
                                } else {
                                    ChatMessageContent(content = state.streamPreview, isUser = false, isStreaming = true, modelLabel = chatModelLabel)
                                }
                            }
                        } else if (message.content.isNotBlank()) {
                            ChatMessageContent(
                                modifier = Modifier.calmItem(this, reduceMotion),
                                content = message.content,
                                isUser = isUser,
                                isStreaming = false,
                                modelLabel = if (!isUser) message.modelLabel.ifBlank { null } else null,
                                onReply = { snippet -> vm.onDraftChange("> ${snippet.take(120)}\n\n") },
                                onEdit = if (isUser && !state.isSending) ({ vm.startEdit(message.id) }) else null,
                                isBeingEdited = message.id == state.editingMessageId,
                                actions =
                                    if (isUser) {
                                        null
                                    } else {
                                        com.skillmcp.mentor.ui.components.chat.ReplyActions(
                                            isSpeaking = state.speakingMessageId == message.id,
                                            onToggleSpeak = {
                                                if (state.speakingMessageId == message.id) vm.stopReadAloud() else vm.readAloud(message.id, message.content)
                                            },
                                            onRegenerate =
                                                if (message.id == lastId && !state.isSending) ({ vm.regenerate(message.id) }) else null,
                                            versionIndex = message.versionIndex,
                                            versionCount = message.versionCount,
                                            onSelectVersion = { vm.selectReplyVersion(message.id, it) },
                                        )
                                    },
                            )
                        }
                    }
                    val lastMessage = state.messages.lastOrNull()
                    if (lastMessage != null && lastMessage.role == "assistant" && !state.isSending &&
                        state.pendingUserMessage == null && state.editingMessageId == null
                    ) {
                        val followUps = com.skillmcp.mentor.mentor.FollowUpSuggestions.forReply(lastMessage.content)
                        if (followUps.isNotEmpty()) {
                            item(key = "followups-${lastMessage.id}") {
                                com.skillmcp.mentor.ui.components.chat.FollowUpChips(
                                    kinds = followUps,
                                    onSend = { prompt ->
                                        hapticView.performSendHaptic()
                                        vm.sendFollowUp(prompt)
                                    },
                                    modifier = Modifier.calmItem(this, reduceMotion).padding(top = 4.dp, bottom = 8.dp),
                                )
                            }
                        }
                    }
                    // Optimistic copy sits at the bottom and shares its key with the persisted row.
                    state.pendingUserMessage?.let { pending ->
                        item(key = state.pendingUserMessageId ?: "pending-user") {
                            // Starts invisible so the entrance actually plays on the first frame after Send.
                            val skipEntrance = reduceMotion || LocalInspectionMode.current
                            val appear =
                                remember { androidx.compose.animation.core.MutableTransitionState(skipEntrance) }
                            appear.targetState = true
                            androidx.compose.animation.AnimatedVisibility(
                                visibleState = appear,
                                modifier = Modifier.calmItem(this, reduceMotion),
                                enter =
                                    slideInVertically(CalmMotion.gentle()) { it / 3 } +
                                        fadeIn(CalmMotion.fastTween(reduceMotion)),
                            ) {
                                ChatMessageContent(
                                    content = pending,
                                    isUser = true,
                                    isStreaming = false,
                                    modelLabel = null,
                                )
                            }
                        }
                    }
                    if (state.showReplySlot) {
                        // Same key from Thinking… through streaming to the persisted reply; content swaps in place.
                        item(key = state.replyKey ?: "reply-slot") {
                            androidx.compose.animation.Crossfade(
                                modifier = Modifier.calmItem(this, reduceMotion),
                                targetState = state.streamPreview.isBlank(),
                                animationSpec = CalmMotion.fastTween(reduceMotion),
                                label = "replySlot",
                            ) { thinking ->
                                if (thinking) {
                                    Column(Modifier.padding(top = 12.dp)) {
                                        com.skillmcp.mentor.ui.components.chat.AssistantHeader(chatModelLabel)
                                        ThinkingShimmerLine()
                                    }
                                } else {
                                    ChatMessageContent(
                                        content = state.streamPreview,
                                        isUser = false,
                                        isStreaming = true,
                                        modelLabel = chatModelLabel,
                                    )
                                }
                            }
                        }
                    }
                }
                if (scrollController.showJumpToLatest) {
                    FloatingActionButton(
                        onClick = {
                            scrollController.followBottom = true
                            scope.launch { scrollController.scrollToBottom(animate = !reduceMotion) }
                        },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.chat_scroll_to_latest))
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
                                Text(stringResource(if (show) R.string.chat_details_hide else R.string.chat_details_show))
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

                state.skillSuggestion?.let { suggestion ->
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    com.skillmcp.mentor.ui.components.chat.SkillSuggestionCard(
                        suggestion = suggestion,
                        onUse = vm::useSuggestedSkill,
                        onDismiss = vm::dismissSkillSuggestion,
                        onOpenDocs = { url -> if (url.startsWith("https://")) uriHandler.openUri(url) },
                        modifier = Modifier.padding(horizontal = MentorDimens.ScreenHorizontal, vertical = 4.dp),
                    )
                }

                if (pendingImageUri != null || !pendingPdf.isNullOrBlank()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MentorDimens.ScreenHorizontal, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(if (pendingImageUri != null) R.string.chat_attached_image else R.string.chat_attached_pdf),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        IconButton(onClick = vm::clearAttachment) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.chat_remove_attachment))
                        }
                    }
                }
                PremiumComposerBar(
                    draft = state.draft,
                    onDraftChange = vm::onDraftChange,
                    onSend = {
                        hapticView.performSendHaptic()
                        vm.sendMessage()
                    },
                    onMic = onMic,
                    onAttach = {
                        attachLauncher.launch(arrayOf("image/*", "application/pdf"))
                    },
                    isSending = state.isSending,
                    isListening = state.isListening,
                    modifier = Modifier.padding(horizontal = MentorDimens.ScreenHorizontal, vertical = 12.dp),
                    onStop = vm::cancelSend,
                    editing = state.editingMessageId != null,
                    onCancelEdit = vm::cancelEdit,
                )
            }
            if (state.isListening) {
                VoiceModeOverlay(
                    transcript = state.draft,
                    listening = true,
                    onDismiss = vm::cancelListening,
                )
            }
        }
    }
}

@Composable
internal fun ConversationDrawerRow(
    chat: UiConversation,
    selected: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onSetTag: () -> Unit,
    onShareMarkdown: () -> Unit,
    onSharePdf: () -> Unit,
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
                            Icons.Rounded.PushPin,
                            contentDescription = stringResource(R.string.chat_pinned),
                            modifier = Modifier.padding(top = 2.dp).size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column {
                        Text(chat.name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        if (chat.folderTag.isNotBlank()) {
                            Text(
                                chat.folderTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            selected = selected,
            onClick = onOpen,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
        )
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.chat_options))
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            ConversationMenuItems(chat, { menuOpen = false }, onRename, onTogglePin, onDelete, onSetTag, onShareMarkdown, onSharePdf)
        }
    }
}

/** Row menu actions (rename, tag, share, pin, delete); internal so snapshot tests can show them inline. */
@Composable
internal fun ConversationMenuItems(
    chat: UiConversation,
    close: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onSetTag: () -> Unit,
    onShareMarkdown: () -> Unit,
    onSharePdf: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.action_rename)) },
        leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) },
        onClick = {
            close()
            onRename()
        },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.chat_tag_title)) },
        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Label, contentDescription = null) },
        onClick = {
            close()
            onSetTag()
        },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.chat_share_markdown)) },
        leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
        onClick = {
            close()
            onShareMarkdown()
        },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.chat_share_pdf)) },
        leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = null) },
        onClick = {
            close()
            onSharePdf()
        },
    )
    DropdownMenuItem(
        text = { Text(stringResource(if (chat.pinned) R.string.chat_unpin else R.string.chat_pin)) },
        leadingIcon = {
            Icon(
                Icons.Rounded.PushPin,
                contentDescription = null,
            )
        },
        onClick = {
            close()
            onTogglePin()
        },
    )
    if (chat.id != "default") {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
                close()
                onDelete()
            },
        )
    }
}

/** animateItem() with the calm spring for placement; disabled entirely under reduced motion. */
private fun Modifier.calmItem(
    scope: androidx.compose.foundation.lazy.LazyItemScope,
    reduceMotion: Boolean,
): Modifier =
    if (reduceMotion) {
        this
    } else {
        with(scope) {
            this@calmItem.animateItem(
                fadeInSpec = androidx.compose.animation.core.tween(CalmMotion.FAST_MS),
                placementSpec = CalmMotion.gentle(),
                fadeOutSpec = androidx.compose.animation.core.tween(CalmMotion.FAST_MS),
            )
        }
    }
