package com.skillmcp.mentor.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.skillmcp.mentor.skills.finder.SkillCategory
import com.skillmcp.mentor.skills.finder.SkillFinderSnapshot
import com.skillmcp.mentor.skills.finder.SkillIndexCodec
import com.skillmcp.mentor.skills.finder.SkillIndexSource
import com.skillmcp.mentor.ui.components.chat.LocalGreetingHour
import com.skillmcp.mentor.ui.screens.SkillFinderContent
import com.skillmcp.mentor.ui.theme.CodeMentorTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Round 9 Skill Finder renders. Uses the real bundled index (assets/official_skills_index.json,
 * built from live GitHub data), so every skill shown is a real official skill.
 */
class Round9SkillFinderSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, showSystemUi = false, maxPercentDifference = 0.1)

    private val index = SkillIndexCodec.parse(File("src/main/assets/official_skills_index.json").readText())!!
    private val skills = SkillIndexCodec.toOfficialSkills(index)
    private val state =
        SkillFinderUiState(
            snapshot =
                SkillFinderSnapshot(
                    skills = skills,
                    source = SkillIndexSource.BUNDLED,
                    indexGeneratedAt = index.generatedAt,
                    lastRefreshAt = 0L,
                ),
        )

    private fun render(
        dark: Boolean,
        content: @Composable () -> Unit,
    ) {
        paparazzi.snapshot {
            CodeMentorTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalGreetingHour provides 9) {
                    Surface(color = MaterialTheme.colorScheme.background) { content() }
                }
            }
        }
    }

    @Composable
    private fun Finder(
        query: String = "",
        category: SkillCategory? = null,
        company: String? = null,
        selectedId: String? = null,
        installed: Set<String> = emptySet(),
    ) {
        SkillFinderContent(
            state = state,
            query = query,
            onQueryChange = {},
            category = category,
            onCategoryChange = {},
            company = company,
            onCompanyChange = {},
            selected = selectedId?.let { id -> skills.first { it.id == id } },
            onSelect = {},
            onCloseDetail = {},
            installedIds = installed,
            onInstall = {},
            onUseInChat = {},
            onOpenLink = {},
            onRefresh = {},
            onBack = {},
        )
    }

    @Test fun finder_search_light() = render(false) { Finder(query = "pdf") }

    @Test fun finder_search_dark() = render(true) { Finder(query = "pdf") }

    @Test fun finder_categories_light() = render(false) { Finder(category = SkillCategory.DESIGN) }

    @Test fun finder_categories_dark() = render(true) { Finder(category = SkillCategory.DESIGN) }

    @Test fun finder_company_filter_light() = render(false) { Finder(company = "Anthropic") }

    @Test fun finder_detail_light() = render(false) { Finder(selectedId = "anthropics/skills:skills/frontend-design") }

    @Test fun finder_detail_dark() = render(true) { Finder(selectedId = "anthropics/skills:skills/frontend-design") }

    @Test fun finder_detail_installed_light() =
        render(false) {
            Finder(
                selectedId = "supabase/agent-skills:skills/supabase-postgres-best-practices",
                installed = setOf("supabase/agent-skills@main:skills/supabase-postgres-best-practices"),
            )
        }

    @Test fun finder_empty_light() = render(false) { Finder(query = "community marketplace plugin") }

    @Test fun finder_empty_dark() = render(true) { Finder(query = "community marketplace plugin") }
}
