package com.skillmcp.mentor.ui.components.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.R
import com.skillmcp.mentor.ui.motion.rememberReduceMotion
import com.skillmcp.mentor.ui.theme.BrandColors
import com.skillmcp.mentor.ui.theme.MentorDimens
import kotlinx.coroutines.launch

private data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val body: Int,
    val icon: ImageVector,
    val accent: Color,
    val asksName: Boolean = false,
)

private val pages =
    listOf(
        OnboardingPage(R.string.onboarding_models_title, R.string.onboarding_models_body, Icons.Rounded.Psychology, BrandColors.Indigo),
        OnboardingPage(R.string.onboarding_private_title, R.string.onboarding_private_body, Icons.Rounded.Lock, Color(0xFF0F766E)),
        OnboardingPage(R.string.onboarding_workflows_title, R.string.onboarding_workflows_body, Icons.Rounded.Mic, BrandColors.CoralDeep),
        OnboardingPage(R.string.onboarding_name_title, R.string.onboarding_name_body, Icons.Rounded.Face, BrandColors.Indigo, asksName = true),
    )

/**
 * Four calm pages. The last one optionally asks for a name (kept on device) so the chat greeting can be personal.
 * [onFinished] receives the trimmed name, or "" when skipped.
 */
@Composable
fun OnboardingFlow(
    onFinished: (name: String) -> Unit,
    initialPage: Int = 0,
) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberReduceMotion()
    var name by rememberSaveable { mutableStateOf("") }
    val last = pagerState.currentPage == pages.lastIndex
    fun finish() = onFinished(name.trim().take(80))

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = MentorDimens.ScreenHorizontal),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            if (!last) {
                TextButton(onClick = ::finish, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Illustration(page.icon, page.accent)
                Spacer(Modifier.height(32.dp))
                Text(
                    stringResource(page.title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(page.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
                if (page.asksName) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(80) },
                        label = { Text(stringResource(R.string.onboarding_name_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions =
                            KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    )
                }
            }
        }
        val pageLabel = stringResource(R.string.onboarding_page_of, pagerState.currentPage + 1, pages.size)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp).semantics { contentDescription = pageLabel },
            horizontalArrangement = Arrangement.Center,
        ) {
            pages.indices.forEach { i ->
                val selected = i == pagerState.currentPage
                val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot")
                val color by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    label = "dotColor",
                )
                Box(Modifier.padding(horizontal = 4.dp).height(8.dp).width(width).clip(CircleShape).background(color))
            }
        }
        Button(
            onClick = {
                if (last) {
                    finish()
                } else {
                    scope.launch {
                        val next = pagerState.currentPage + 1
                        if (reduceMotion) pagerState.scrollToPage(next) else pagerState.animateScrollToPage(next)
                    }
                }
            },
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(bottom = 16.dp),
        ) {
            Text(
                stringResource(if (last) R.string.onboarding_get_started else R.string.onboarding_next),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Layered tonal circles + a gradient tile: a lightweight, vector-only illustration. */
@Composable
private fun Illustration(
    icon: ImageVector,
    accent: Color,
) {
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(200.dp).clip(CircleShape).background(accent.copy(alpha = 0.07f)))
        Box(Modifier.size(148.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)))
        Box(
            Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(accent, BrandColors.Coral))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = 26.dp).size(14.dp).clip(CircleShape).background(BrandColors.Coral),
        )
        Box(
            Modifier.align(Alignment.BottomStart).padding(bottom = 30.dp, start = 22.dp).size(10.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
        )
    }
}
