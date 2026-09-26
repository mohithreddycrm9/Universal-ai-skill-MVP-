package com.skillmcp.mentor.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                tonalElevation = 6.dp,
            ) {
                MentorTab.entries.forEach { tab ->
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
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
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
            composable(MentorTab.Models.route) { ModelsScreen(vm) }
            composable(MentorTab.Usage.route) { UsageScreen(vm) }
            composable(MentorTab.Skills.route) { SkillsScreen(vm) }
            composable(MentorTab.Settings.route) { SettingsScreen(vm) }
        }
    }
}

private fun tabIcon(tab: MentorTab): ImageVector =
    when (tab) {
        MentorTab.Chat -> Icons.AutoMirrored.Filled.Chat
        MentorTab.Models -> Icons.Outlined.Hub
        MentorTab.Usage -> Icons.Outlined.BarChart
        MentorTab.Skills -> Icons.Outlined.AutoAwesome
        MentorTab.Settings -> Icons.Outlined.Settings
    }
