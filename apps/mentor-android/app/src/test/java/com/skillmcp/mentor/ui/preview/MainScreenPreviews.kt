package com.skillmcp.mentor.ui.preview

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.core.app.ActivityOptionsCompat
import com.skillmcp.mentor.llm.defaultLlmProfiles
import com.skillmcp.mentor.mentor.UiMessage
import com.skillmcp.mentor.ui.MentorUiState
import com.skillmcp.mentor.ui.chat.PreviewChatActions
import com.skillmcp.mentor.ui.screens.ChatScreenContent
import com.skillmcp.mentor.ui.screens.DiscoverScreenContent
import com.skillmcp.mentor.ui.screens.SettingsScreenContent
import com.skillmcp.mentor.ui.settings.PreviewSettingsActions
import com.skillmcp.mentor.ui.theme.CodeMentorTheme

object MainScreenPreviewSamples {
    fun chatState(): MentorUiState =
        MentorUiState(
            messages =
                listOf(
                    UiMessage("1", "user", "Help me plan meals for the week."),
                    UiMessage(
                        "2",
                        "assistant",
                        "Here is a simple plan with vegetarian options and a grocery list.",
                    ),
                ),
            conversations = listOf(com.skillmcp.mentor.mentor.UiConversation("c1", "Meals this week", 0L)),
            activeConversationId = "c1",
            activeLlmProfile = defaultLlmProfiles().first(),
            draft = "Add lunch ideas",
        )

    fun discoverState(): MentorUiState =
        MentorUiState(
            activeLlmProfile = defaultLlmProfiles().first(),
            usageTotals =
                com.skillmcp.mentor.llm.UsageTotals(
                    requestCount = 12,
                    promptTokens = 4000,
                    completionTokens = 900,
                    estimatedUsd = 0.0,
                ),
        )

    fun settingsState(): MentorUiState =
        MentorUiState(
            activeLlmProfile = defaultLlmProfiles().first(),
            prefs =
                com.skillmcp.mentor.data.MentorPrefs(
                    requireBiometricUnlock = false,
                    dailyBriefReminder = true,
                    morningBriefCalendar = true,
                ),
        )
}

@Composable
fun ChatScreenPreview(darkTheme: Boolean) {
    CodeMentorTheme(darkTheme = darkTheme) {
        ChatScreenPreviewContent(state = MainScreenPreviewSamples.chatState())
    }
}

/** Renders the real chat screen (drawer, top bar, bubbles, composer) with no-op actions. */
@Composable
internal fun ChatScreenPreviewContent(state: MentorUiState) {
    PreviewHost {
        ChatScreenContent(
            state = state,
            searchResults = null,
            pendingImageUri = null,
            pendingPdf = null,
            vm = PreviewChatActions,
        )
    }
}

@Composable
fun DiscoverScreenPreview(darkTheme: Boolean) {
    CodeMentorTheme(darkTheme = darkTheme) {
        DiscoverScreenContent(
            state = MainScreenPreviewSamples.discoverState(),
            onConnect = {},
            onPopularUseCase = {},
            onOpenUsage = {},
            onOpenModels = {},
        )
    }
}

/** Renders the real settings screen with no-op actions. */
@Composable
fun SettingsScreenPreview(darkTheme: Boolean) {
    CodeMentorTheme(darkTheme = darkTheme) {
        PreviewHost {
            SettingsScreenContent(
                state = MainScreenPreviewSamples.settingsState(),
                backupPrompt = false,
                restorePrompt = false,
                vm = PreviewSettingsActions,
            )
        }
    }
}

/**
 * Previews have no Activity: provide a no-op ActivityResultRegistry so screens that register
 * permission/document launchers can compose.
 */
@Composable
private fun PreviewHost(content: @Composable () -> Unit) {
    if (LocalActivityResultRegistryOwner.current != null) {
        content()
        return
    }
    val owner =
        remember {
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry =
                    object : ActivityResultRegistry() {
                        override fun <I, O> onLaunch(
                            requestCode: Int,
                            contract: ActivityResultContract<I, O>,
                            input: I,
                            options: ActivityOptionsCompat?,
                        ) = Unit
                    }
            }
        }
    CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner, content = content)
}
