package com.skillmcp.mentor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.skillmcp.mentor.data.AppContainer
import com.skillmcp.mentor.security.BiometricGate
import com.skillmcp.mentor.ui.components.ConnectLlmSheet
import com.skillmcp.mentor.ui.components.FloatingMentorNavBar
import com.skillmcp.mentor.ui.components.GuidedSetupSheet
import com.skillmcp.mentor.ui.components.SkillInstallDialog
import com.skillmcp.mentor.ui.components.onboarding.OnboardingFlow
import com.skillmcp.mentor.ui.screens.ChatScreen
import com.skillmcp.mentor.ui.screens.DiscoverScreen
import com.skillmcp.mentor.ui.screens.ModelsScreen
import com.skillmcp.mentor.ui.screens.PersonalizationScreen
import com.skillmcp.mentor.ui.screens.SettingsScreen
import com.skillmcp.mentor.ui.screens.SkillsScreen
import com.skillmcp.mentor.ui.screens.ActivityScreen

enum class MentorTab(val route: String, val label: String, val showInBar: Boolean = true) {
    Chat("chat", "Chat"),
    Discover("discover", "Discover"),
    Models("models", "Models", showInBar = false),
    Usage("usage", "Activity", showInBar = false),
    Skills("skills", "Abilities", showInBar = false),
    Personalization("personalization", "Personalization", showInBar = false),
    Settings("settings", "Settings"),
}

fun MentorTab.barParentRoute(): String =
    when (this) {
        MentorTab.Models, MentorTab.Usage, MentorTab.Skills -> MentorTab.Discover.route
        MentorTab.Personalization -> MentorTab.Settings.route
        else -> route
    }

@Composable
fun MentorTab.localizedLabel(): String =
    when (this) {
        MentorTab.Chat -> stringResource(R.string.nav_chat)
        MentorTab.Discover -> stringResource(R.string.nav_discover)
        MentorTab.Settings -> stringResource(R.string.nav_settings)
        else -> label
    }

@Composable
fun MentorApp(container: AppContainer) {
    val vm: MentorViewModel =
        viewModel(factory = MentorViewModel.Factory(container, container.appContext))
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: MentorTab.Chat.route
    val state by vm.uiState.collectAsState()
    val guidedVisible by vm.guidedSetupVisible.collectAsState()
    val connectProfileId by vm.connectLlmProfileId.collectAsState()
    val connectProfile = state.llmProfiles.find { it.id == connectProfileId }
    var advancedParentTab by remember { mutableStateOf(MentorTab.Discover.route) }

    val highlightedTab =
        when (current) {
            MentorTab.Models.route, MentorTab.Usage.route, MentorTab.Skills.route -> advancedParentTab
            else -> MentorTab.entries.find { it.route == current }?.barParentRoute() ?: current
        }

    LaunchedEffect(vm) {
        vm.openChatRequests.collect {
            nav.navigate(MentorTab.Chat.route) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(vm) {
        vm.openTabRequests.collect { route ->
            val from = nav.currentBackStackEntry?.destination?.route ?: MentorTab.Chat.route
            if (route in listOf("models", "usage", "skills")) {
                advancedParentTab =
                    if (from == MentorTab.Settings.route) {
                        MentorTab.Settings.route
                    } else {
                        MentorTab.Discover.route
                    }
            }
            val dest = MentorTab.entries.find { it.route == route }?.route ?: MentorTab.Chat.route
            nav.navigate(dest) {
                launchSingleTop = true
            }
        }
    }

    val onAdvancedBack: () -> Unit = { nav.popBackStack() }

    if (current in listOf("models", "usage", "skills", "personalization")) {
        BackHandler(onBack = onAdvancedBack)
    }

    BiometricGate(
        enabled = state.prefs.requireBiometricUnlock,
        onLockUnavailable = vm::onAppLockUnavailable,
    ) {
        if (!state.prefs.hasSeenWelcome) {
            OnboardingFlow(onFinished = vm::finishWelcome)
            return@BiometricGate
        }
        GuidedSetupSheet(
            visible = !state.prefs.hasCompletedGuidedSetup && guidedVisible && connectProfileId == null,
            useCases = state.rankedUseCases,
            onConnectOpenAi = {
                vm.hideGuidedSetupForConnect()
                vm.openConnectLlm("openai")
            },
            onSendTestMessage = {
                vm.hideGuidedSetupForConnect()
                vm.openChatWithSuggestion("Say hello in one sentence — this is my setup test.")
            },
            onPickUseCase = { vm.startPopularUseCase(it) },
            onFinish = vm::completeGuidedSetup,
        )
        SkillInstallDialog(
            request = state.pendingSkillInstall,
            onConfirm = vm::confirmSkillInstall,
            onDismiss = vm::dismissSkillInstall,
        )
        ConnectLlmSheet(
            profile = connectProfile,
            vm = vm,
            onDismiss = vm::dismissConnectLlm,
        )
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                // Inside a conversation the chat gets the full height (thin top bar + composer only);
                // the drawer links to Discover/Settings, and "New chat" brings the tab bar back.
                val inConversation =
                    isInConversation(current, state.messages.isNotEmpty(), state.isSending)
                androidx.compose.animation.AnimatedVisibility(
                    visible = !inConversation,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
                ) {
                    FloatingMentorNavBar(
                        selectedRoute = highlightedTab,
                        onSelect = { tab ->
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        tabLabel = { it.localizedLabel() },
                    )
                }
            },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = MentorTab.Chat.route,
                modifier = Modifier.padding(padding),
            ) {
                composable(MentorTab.Chat.route) { ChatScreen(vm) }
                composable(MentorTab.Discover.route) { DiscoverScreen(vm) }
                composable(MentorTab.Models.route) { ModelsScreen(vm, onBack = onAdvancedBack) }
                composable(MentorTab.Usage.route) { ActivityScreen(vm, onBack = onAdvancedBack) }
                composable(MentorTab.Skills.route) { SkillsScreen(vm, onBack = onAdvancedBack) }
                composable(MentorTab.Settings.route) { SettingsScreen(vm) }
                composable(MentorTab.Personalization.route) { PersonalizationScreen(vm, onBack = onAdvancedBack) }
            }
        }
    }
}

/** The bottom tab bar is hidden while a conversation (messages or an in-flight send) is on the chat tab. */
fun isInConversation(currentRoute: String?, hasMessages: Boolean, isSending: Boolean): Boolean =
    currentRoute == MentorTab.Chat.route && (hasMessages || isSending)
