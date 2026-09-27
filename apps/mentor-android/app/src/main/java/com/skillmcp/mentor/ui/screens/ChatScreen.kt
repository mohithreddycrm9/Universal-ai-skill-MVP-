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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Explore
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
    val filteredChats =
        searchResults
            ?: state.conversations.filter { it.name.contains(drawerQuery, ignoreCase = true) }
    val drawerSections = groupConversationsForDrawer(filteredChats)
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
                    "Shared content will be sent to ${state.activeLlmProfile?.name ?: "your connected model"} when you tap Send. " +
                        "The provider may process URLs and text on their servers.",
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
                        onDelete = { vm.deleteConversation(chat.id) },
                        onSetTag = {
                            tagTarget = chat
                            tagDraft = chat.folderTag
                        },
                        onShareMarkdown = { vm.shareChatMarkdown(chat.id) },
                        onSharePdf = { vm.shareChatPdf(chat.id) },
                    )
                },
                // The tab bar is hidden inside a conversation; the drawer keeps the other tabs one tap away.
                footer = {
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    androidx.compose.material3.NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_discover)) },
                        selected = false,
                        icon = { Icon(Icons.Outlined.Explore, contentDescription = null) },
                        onClick = {
                            scope.launch { drawer.close() }
                            vm.requestOpenTab("discover")
                        },
                        modifier = Modifier.padding(androidx.compose.material3.NavigationDrawerItemDefaults.ItemPadding),
                    )
                    androidx.compose.material3.NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_settings)) },
                        selected = false,
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
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
                TopAppBar(
                    title = {
                        Text(
                            chatTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawer.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.chat_open_chats))
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
                                state.messageAllowance.message ?: "Message limit reached.",
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
                    val profile = state.activeLlmProfile
                    items(state.messages, key = { it.id }) { message ->
                        if (message.content.isNotBlank()) {
                            val isUser = message.role == "user"
                            ChatMessageContent(
                                modifier = calmItemModifier(reduceMotion),
                                content = message.content,
                                isUser = isUser,
                                isStreaming = false,
                                modelLabel = if (!isUser) profile?.name else null,
                                onReply = { snippet -> vm.onDraftChange("> ${snippet.take(120)}\n\n") },
                            )
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
                                modifier = calmItemModifier(reduceMotion),
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
                                modifier = calmItemModifier(reduceMotion),
                                targetState = state.streamPreview.isBlank(),
                                animationSpec = CalmMotion.fastTween(reduceMotion),
                                label = "replySlot",
                            ) { thinking ->
                                if (thinking) {
                                    ThinkingShimmerLine()
                                } else {
                                    ChatMessageContent(
                                        content = state.streamPreview,
                                        isUser = false,
                                        isStreaming = true,
                                        modelLabel = profile?.name,
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
                        Icon(Icons.Filled.ExpandMore, contentDescription = stringResource(R.string.chat_scroll_to_latest))
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
                            if (pendingImageUri != null) "Image attached" else "PDF text ready to send",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        IconButton(onClick = vm::clearAttachment) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.chat_remove_attachment))
                        }
                    }
                }
                if (state.isSending) {
                    androidx.compose.material3.TextButton(
                        onClick = vm::cancelSend,
                        modifier = Modifier.padding(horizontal = MentorDimens.ScreenHorizontal),
                    ) {
                        Text(stringResource(R.string.chat_stop))
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
private fun ConversationDrawerRow(
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
                            Icons.Default.PushPin,
                            contentDescription = stringResource(R.string.chat_pinned),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Column {
                        Text(chat.name, maxLines = 1)
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
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.chat_options))
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rename)) },
                onClick = {
                    menuOpen = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_tag_title)) },
                onClick = {
                    menuOpen = false
                    onSetTag()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_share_markdown)) },
                onClick = {
                    menuOpen = false
                    onShareMarkdown()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_share_pdf)) },
                onClick = {
                    menuOpen = false
                    onSharePdf()
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
                    text = { Text(stringResource(R.string.action_delete)) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}

/** animateItem() with the calm spring for placement; disabled entirely under reduced motion. */
private fun androidx.compose.foundation.lazy.LazyItemScope.calmItemModifier(reduceMotion: Boolean): Modifier =
    if (reduceMotion) {
        Modifier
    } else {
        Modifier.animateItem(
            fadeInSpec = androidx.compose.animation.core.tween(CalmMotion.FAST_MS),
            placementSpec = CalmMotion.gentle(),
            fadeOutSpec = androidx.compose.animation.core.tween(CalmMotion.FAST_MS),
        )
    }
