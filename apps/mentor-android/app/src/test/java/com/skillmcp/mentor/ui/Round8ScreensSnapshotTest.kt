package com.skillmcp.mentor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.skillmcp.mentor.data.MentorPrefs
import com.skillmcp.mentor.llm.LlmProfile
import com.skillmcp.mentor.llm.ROLE_MODEL_SWITCH
import com.skillmcp.mentor.llm.defaultLlmProfiles
import com.skillmcp.mentor.mentor.UiConversation
import com.skillmcp.mentor.mentor.UiMessage
import com.skillmcp.mentor.mentor.UseCaseCatalog
import com.skillmcp.mentor.ui.components.chat.ChatHistoryDrawer
import com.skillmcp.mentor.ui.components.chat.LocalGreetingHour
import com.skillmcp.mentor.ui.components.chat.ModelSwitcherContent
import com.skillmcp.mentor.ui.components.chat.groupConversationsForDrawer
import com.skillmcp.mentor.ui.components.onboarding.OnboardingFlow
import com.skillmcp.mentor.ui.preview.ChatScreenPreviewContent
import com.skillmcp.mentor.ui.screens.ConversationDrawerRow
import com.skillmcp.mentor.ui.screens.ConversationMenuItems
import com.skillmcp.mentor.ui.screens.DiscoverScreenContent
import com.skillmcp.mentor.ui.screens.PersonalizationScreenContent
import com.skillmcp.mentor.ui.settings.PreviewSettingsActions
import com.skillmcp.mentor.ui.preview.SettingsScreenPreview
import com.skillmcp.mentor.ui.theme.CodeMentorTheme
import org.junit.Rule
import org.junit.Test

/**
 * Round 8 renders (normal font size, light + dark). Sample conversations live here in src/test only.
 * Popups (drop-down menu, bottom sheet) can't be drawn by Paparazzi, so those two renders place the
 * real menu items / sheet content inline over the screen.
 */
class Round8ScreensSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, showSystemUi = false, maxPercentDifference = 0.1)

    private val profiles: List<LlmProfile> =
        defaultLlmProfiles().map {
            when (it.id) {
                "openai" -> it.copy(apiKey = "sk-preview")
                "anthropic" -> it.copy(apiKey = "preview")
                "ollama" -> it.copy(baseUrl = "http://192.168.1.20:11434/")
                else -> it
            }
        }
    private val openAi = profiles.first { it.id == "openai" }
    private val ollama = profiles.first { it.id == "ollama" }
    private val chats =
        listOf(
            UiConversation("c1", "Weekly meal plan", 1_000L, llmProfileId = "openai"),
            UiConversation("c2", "Trip to Goa", 900L, pinned = true),
        )

    private fun base(messages: List<UiMessage> = emptyList(), active: LlmProfile = openAi) =
        MentorUiState(
            messages = messages,
            conversations = chats,
            activeConversationId = "c1",
            llmProfiles = profiles,
            activeLlmProfile = active,
            prefs = MentorPrefs(displayName = "Asha Rao", hasSeenWelcome = true, hasCompletedGuidedSetup = true),
        )

    private val reply =
        """
        Here's a light plan for the week:

        - **Mon:** dal, rice and a cucumber salad
        - **Tue:** vegetable pulao with raita
        - **Wed:** paneer wraps

        ```python
        groceries = ["dal", "rice", "paneer"]
        ```
        """.trimIndent()

    private val bubbles =
        listOf(
            UiMessage("u1", "user", "Plan simple vegetarian dinners for this week."),
            UiMessage("a1", "assistant", reply, versionCount = 2, versionIndex = 1, modelLabel = "gpt-4o-mini"),
        )

    private fun render(dark: Boolean, content: @Composable () -> Unit) =
        paparazzi.snapshot {
            CodeMentorTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalGreetingHour provides 9) {
                    Surface(color = MaterialTheme.colorScheme.background) { content() }
                }
            }
        }

    // 1. Empty chat: greeting with the user's first name and suggestion cards.
    @Test fun emptyChat_light() = render(false) { ChatScreenPreviewContent(base()) }
    @Test fun emptyChat_dark() = render(true) { ChatScreenPreviewContent(base()) }

    // 2. Bubbles + reply action row (version 2/2, copy, read aloud, regenerate, share).
    @Test fun chatBubbles_light() = render(false) { ChatScreenPreviewContent(base(bubbles)) }
    @Test fun chatBubbles_dark() = render(true) { ChatScreenPreviewContent(base(bubbles)) }

    // 3. Follow-up chips after a short reply.
    private val followUps =
        listOf(
            UiMessage("u1", "user", "What is a good first habit for better sleep?"),
            UiMessage("a1", "assistant", "Keep the same wake-up time every day, even on weekends. It anchors your body clock.", modelLabel = "gpt-4o-mini"),
        )
    @Test fun followUps_light() = render(false) { ChatScreenPreviewContent(base(followUps)) }
    @Test fun followUps_dark() = render(true) { ChatScreenPreviewContent(base(followUps)) }

    // 4. Edit flow: the bubble being edited is tinted and the composer shows the editing banner.
    private fun editing() = base(followUps).copy(editingMessageId = "u1", draft = "What is a good first habit for deeper sleep?")
    @Test fun editMessage_light() = render(false) { ChatScreenPreviewContent(editing()) }
    @Test fun editMessage_dark() = render(true) { ChatScreenPreviewContent(editing()) }

    // 5. Two models in one chat: captions per reply and the "Switched to" divider.
    private val twoModels =
        listOf(
            UiMessage("u1", "user", "Suggest a 3-day Goa itinerary."),
            UiMessage("a1", "assistant", "Day 1: North Goa beaches. Day 2: Old Goa churches. Day 3: spice farm and Palolem.", modelLabel = "gpt-4o-mini"),
            UiMessage("s1", ROLE_MODEL_SWITCH, "llama3.2", modelLabel = "llama3.2"),
            UiMessage("u2", "user", "Make day 3 cheaper."),
            UiMessage("a2", "assistant", "Swap the spice farm tour for a free walk around Fontainhas, then take the local bus to Palolem.", modelLabel = "llama3.2"),
        )
    private fun twoModelState() = base(twoModels, active = ollama).copy(conversations = chats.map { if (it.id == "c1") it.copy(llmProfileId = "ollama") else it })
    @Test fun twoModels_light() = render(false) { ChatScreenPreviewContent(twoModelState()) }
    @Test fun twoModels_dark() = render(true) { ChatScreenPreviewContent(twoModelState()) }

    // 6. Model switcher sheet over the chat.
    @Composable
    private fun SwitcherOverChat() {
        Box(Modifier.fillMaxSize()) {
            ChatScreenPreviewContent(base(bubbles))
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)))
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Column(Modifier.padding(top = 20.dp)) {
                    ModelSwitcherContent(profiles, currentId = "openai", onPick = {}, onConnect = {}, onManage = {})
                }
            }
        }
    }
    @Test fun modelSwitcher_light() = render(false) { SwitcherOverChat() }
    @Test fun modelSwitcher_dark() = render(true) { SwitcherOverChat() }

    // 7. Drawer: pinned section, and the real row menu (rename / pin / …) drawn inline.
    @Composable
    private fun DrawerWithMenu() {
        val now = 1_700_000_000_000L
        val day = 24L * 60 * 60 * 1000
        val list =
            listOf(
                UiConversation("c2", "Trip to Goa", now - 2 * day, pinned = true),
                UiConversation("c1", "Weekly meal plan", now - 60_000),
                UiConversation("c3", "Reply to landlord", now - 3_600_000),
                UiConversation("c4", "Sleep habits", now - day - 1000),
                UiConversation("c5", "Resume bullets", now - 9 * day),
            )
        Box(Modifier.fillMaxSize()) {
            ChatHistoryDrawer(
                query = "",
                onQueryChange = {},
                sections = groupConversationsForDrawer(list, now),
                activeId = "c1",
                onNewChat = {},
                onOpenChat = {},
                conversationRow = { chat, selected, onClick ->
                    ConversationDrawerRow(chat, selected, onClick, {}, {}, {}, {}, {}, {})
                },
            )
            Surface(
                modifier = Modifier.offset(x = 170.dp, y = 250.dp).width(220.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    ConversationMenuItems(list[0], {}, {}, {}, {}, {}, {}, {})
                }
            }
        }
    }
    @Test fun drawerPinRename_light() = render(false) { DrawerWithMenu() }
    @Test fun drawerPinRename_dark() = render(true) { DrawerWithMenu() }

    // 8. Discover with visual cards.
    @Composable
    private fun Discover() =
        DiscoverScreenContent(
            state = base().copy(rankedUseCases = UseCaseCatalog.featured),
            onConnect = {},
            onPopularUseCase = {},
            onOpenUsage = {},
            onOpenModels = {},
        )
    @Test fun discover_light() = render(false) { Discover() }
    @Test fun discover_dark() = render(true) { Discover() }

    // 9. Settings (grouped, with icons).
    @Test fun settings_light() = paparazzi.snapshot { SettingsScreenPreview(darkTheme = false) }
    @Test fun settings_dark() = paparazzi.snapshot { SettingsScreenPreview(darkTheme = true) }

    // 10. Personalization.
    private val personalPrefs =
        MentorPrefs(
            displayName = "Asha",
            aboutMe = "I'm a product designer in Bengaluru. I cook vegetarian food and I'm learning Kannada.",
            responseInstructions = "Be concise. Use bullet points for steps and metric units.",
            personalizationEnabled = true,
        )
    @Test fun personalization_light() = render(false) { PersonalizationScreenContent(personalPrefs, PreviewSettingsActions, onBack = {}) }
    @Test fun personalization_dark() = render(true) { PersonalizationScreenContent(personalPrefs, PreviewSettingsActions, onBack = {}) }

    // 11. Onboarding: first page and the optional name page.
    @Test fun onboarding_light() = render(false) { OnboardingFlow(onFinished = {}) }
    @Test fun onboarding_dark() = render(true) { OnboardingFlow(onFinished = {}) }
    @Test fun onboardingName_light() = render(false) { OnboardingFlow(onFinished = {}, initialPage = 3) }
    @Test fun onboardingName_dark() = render(true) { OnboardingFlow(onFinished = {}, initialPage = 3) }
}
