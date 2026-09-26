package com.skillmcp.mentor.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.skillmcp.mentor.data.AppContainer
import com.skillmcp.mentor.security.BiometricGate
import com.skillmcp.mentor.ui.components.ConnectLlmSheet
import com.skillmcp.mentor.ui.components.GuidedSetupSheet
import com.skillmcp.mentor.ui.components.SkillInstallDialog
import com.skillmcp.mentor.ui.screens.ChatScreen
import com.skillmcp.mentor.ui.screens.DiscoverScreen
import com.skillmcp.mentor.ui.screens.ModelsScreen
import com.skillmcp.mentor.ui.screens.SettingsScreen
import com.skillmcp.mentor.ui.screens.SkillsScreen
import com.skillmcp.mentor.ui.screens.UsageScreen

enum class MentorTab(val route: String, val label: String, val showInBar: Boolean = true) {
    Chat("chat", "Chat"),
    Discover("discover", "Discover"),
    Models("models", "Models", showInBar = false),
    Usage("usage", "Usage", showInBar = false),
    Skills("skills", "Abilities", showInBar = false),
    Settings("settings", "Settings"),
}

@Composable
fun MentorApp(container: AppContainer) {
    val vm: MentorViewModel =
        viewModel(factory = MentorViewModel.Factory(container, container.appContext))
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: MentorTab.Chat.route
    val state by vm.uiState.collectAsState()
    val connectProfileId by vm.connectLlmProfileId.collectAsState()
    val connectProfile = state.llmProfiles.find { it.id == connectProfileId }

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
            val dest = MentorTab.entries.find { it.route == route }?.route ?: MentorTab.Chat.route
            nav.navigate(dest) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    BiometricGate(enabled = state.prefs.requireBiometricUnlock) {
        GuidedSetupSheet(
            visible = !state.prefs.hasCompletedGuidedSetup,
            useCases = state.rankedUseCases,
            onConnectOpenAi = { vm.openConnectLlm("openai") },
            onSendTestMessage = {
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
            containerColor = MaterialTheme.colorScheme.surface,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    MentorTab.entries.filter { it.showInBar }.forEach { tab ->
                        val selected = current == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tabIcon(tab), contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors =
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                        )
                    }
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
                composable(MentorTab.Models.route) { ModelsScreen(vm) }
                composable(MentorTab.Usage.route) { UsageScreen(vm) }
                composable(MentorTab.Skills.route) { SkillsScreen(vm) }
                composable(MentorTab.Settings.route) { SettingsScreen(vm) }
            }
        }
    }
}

private fun tabIcon(tab: MentorTab): ImageVector =
    when (tab) {
        MentorTab.Chat -> Icons.AutoMirrored.Filled.Chat
        MentorTab.Discover -> Icons.Outlined.Explore
        MentorTab.Models -> Icons.Outlined.Hub
        MentorTab.Usage -> Icons.Outlined.BarChart
        MentorTab.Skills -> Icons.Outlined.AutoAwesome
        MentorTab.Settings -> Icons.Outlined.Settings
    }
