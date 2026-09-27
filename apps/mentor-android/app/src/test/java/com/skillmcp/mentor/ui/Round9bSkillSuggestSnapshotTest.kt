package com.skillmcp.mentor.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.skillmcp.mentor.data.MentorPrefs
import com.skillmcp.mentor.llm.LlmProfile
import com.skillmcp.mentor.llm.defaultLlmProfiles
import com.skillmcp.mentor.mentor.UiConversation
import com.skillmcp.mentor.mentor.UiMessage
import com.skillmcp.mentor.skills.finder.OfficialOrgAllowlist
import com.skillmcp.mentor.skills.finder.SkillIndexCodec
import com.skillmcp.mentor.skills.finder.SkillSuggestionDecision
import com.skillmcp.mentor.skills.finder.SkillSuggestionEngine
import com.skillmcp.mentor.ui.chat.SkillSuggestionItem
import com.skillmcp.mentor.ui.chat.SkillSuggestionUi
import com.skillmcp.mentor.ui.components.chat.LocalGreetingHour
import com.skillmcp.mentor.ui.preview.ChatScreenPreviewContent
import com.skillmcp.mentor.ui.preview.SettingsScreenPreview
import com.skillmcp.mentor.ui.theme.CodeMentorTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Round 9b renders: the "Ready: <official skill>" card for a build request. The suggestion comes
 * from the real engine over the real bundled index; the chat text itself is test-only sample data.
 */
class Round9bSkillSuggestSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, showSystemUi = false, maxPercentDifference = 0.1)

    private val skills = SkillIndexCodec.toOfficialSkills(SkillIndexCodec.parse(File("src/main/assets/official_skills_index.json").readText())!!)
    private val profiles: List<LlmProfile> = defaultLlmProfiles().map { if (it.id == "openai") it.copy(apiKey = "sk-preview") else it }
    private val request = "Build a PDF report of my monthly expenses"
    private val messages =
        listOf(
            UiMessage("u1", "user", request),
            UiMessage(
                "a1",
                "assistant",
                "Sure. Share the expense list (date, category, amount) and I'll group it by category, add totals and a short summary, then lay it out as a one-page PDF report.",
                modelLabel = "gpt-4o-mini",
            ),
        )

    private fun state(suggestion: SkillSuggestionUi?, msgs: List<UiMessage> = messages, title: String = "Expense report") =
        MentorUiState(
            messages = msgs,
            conversations = listOf(UiConversation("c1", title, 1_000L)),
            activeConversationId = "c1",
            llmProfiles = profiles,
            activeLlmProfile = profiles.first { it.id == "openai" },
            prefs = MentorPrefs(hasSeenWelcome = true, hasCompletedGuidedSetup = true),
            skillSuggestion = suggestion,
        )

    private fun ready(): SkillSuggestionUi {
        val decision = SkillSuggestionEngine.decide(request, skills, true, emptySet(), emptySet()) as SkillSuggestionDecision.Ready
        return SkillSuggestionUi("c1", items = listOf(SkillSuggestionItem(decision.skills.first(), prefetched = true)))
    }

    private fun active() = ready().let { it.copy(items = it.items.map { i -> i.copy(active = true) }) }

    private val serviceNowMessages =
        listOf(
            UiMessage("u1", "user", "Build a ServiceNow app for incident triage"),
            UiMessage("a1", "assistant", "Happy to help plan it. Which ServiceNow release are you on, and should triage run on new incidents only?", modelLabel = "gpt-4o-mini"),
        )

    private fun noServiceNow() =
        SkillSuggestionUi(
            "c1",
            noOfficialCompany = "ServiceNow",
            docsUrl = OfficialOrgAllowlist.orgsOf("ServiceNow").first().docsUrl,
        )

    private fun render(dark: Boolean, content: @Composable () -> Unit) =
        paparazzi.snapshot {
            CodeMentorTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalGreetingHour provides 9) {
                    Surface(color = MaterialTheme.colorScheme.background) { content() }
                }
            }
        }

    @Test fun chat_build_ready_light() = render(false) { ChatScreenPreviewContent(state(ready())) }

    @Test fun chat_build_ready_dark() = render(true) { ChatScreenPreviewContent(state(ready())) }

    @Test fun chat_build_active_light() = render(false) { ChatScreenPreviewContent(state(active())) }

    @Test fun chat_build_active_dark() = render(true) { ChatScreenPreviewContent(state(active())) }

    @Test fun chat_servicenow_none_light() = render(false) { ChatScreenPreviewContent(state(noServiceNow(), serviceNowMessages, "Incident triage app")) }

    @Test fun chat_servicenow_none_dark() = render(true) { ChatScreenPreviewContent(state(noServiceNow(), serviceNowMessages, "Incident triage app")) }

    @Test fun settings_suggest_toggle_light() = paparazzi.snapshot { SettingsScreenPreview(darkTheme = false) }

    @Test fun settings_suggest_toggle_dark() = paparazzi.snapshot { SettingsScreenPreview(darkTheme = true) }
}
