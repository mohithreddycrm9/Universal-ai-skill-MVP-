package com.skillmcp.mentor.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.skillmcp.mentor.data.AppContainer
import com.skillmcp.mentor.ui.screens.ChatScreen
import com.skillmcp.mentor.ui.screens.ModelsScreen
import com.skillmcp.mentor.ui.screens.SettingsScreen
import com.skillmcp.mentor.ui.screens.SkillsScreen
import com.skillmcp.mentor.ui.screens.UsageScreen

enum class MentorTab(val route: String, val label: String) {
    Chat("chat", "Chat"),
    Models("models", "Models"),
    Usage("usage", "Usage"),
    Skills("skills", "Skills"),
    Settings("settings", "Settings"),
}

@Composable
fun MentorApp(container: AppContainer) {
    val vm: MentorViewModel =
        viewModel(factory = MentorViewModel.Factory(container, container.appContext))
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: MentorTab.Chat.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                MentorTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                when (tab) {
                                    MentorTab.Chat -> Icons.AutoMirrored.Filled.Chat
                                    MentorTab.Models -> Icons.Default.Hub
                                    MentorTab.Usage -> Icons.Default.BarChart
                                    MentorTab.Skills -> Icons.Default.Extension
                                    MentorTab.Settings -> Icons.Default.Settings
                                },
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
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
            composable(MentorTab.Models.route) { ModelsScreen(vm) }
            composable(MentorTab.Usage.route) { UsageScreen(vm) }
            composable(MentorTab.Skills.route) { SkillsScreen(vm) }
            composable(MentorTab.Settings.route) { SettingsScreen(vm) }
        }
    }
}
