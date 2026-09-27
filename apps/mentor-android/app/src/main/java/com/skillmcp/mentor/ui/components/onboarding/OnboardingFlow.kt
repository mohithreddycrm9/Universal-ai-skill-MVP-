package com.skillmcp.mentor.ui.components.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.ui.theme.BrandColors
import com.skillmcp.mentor.ui.theme.MentorDimens
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val body: String,
    val icon: ImageVector,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingFlow(onFinished: () -> Unit) {
    val pages =
        listOf(
            OnboardingPage(
                "Your AI, your choice of model",
                "Connect OpenAI, Google, Anthropic, Hugging Face, or Ollama on your network.",
                Icons.Outlined.Psychology,
            ),
            OnboardingPage(
                "Private by design",
                "Keys stay encrypted on device. You choose what syncs or backs up.",
                Icons.Outlined.Lock,
            ),
            OnboardingPage(
                "Workflows that save time",
                "Discover starters for study, work, meals, and travel — then chat in seconds.",
                Icons.Outlined.Speed,
            ),
        )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = MentorDimens.ScreenHorizontal),
    ) {
        LinearProgressIndicator(
            progress = { (pagerState.currentPage + 1f) / pages.size },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            color = BrandColors.Indigo,
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            val data = pages[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    data.icon,
                    contentDescription = null,
                    modifier = Modifier.height(96.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    data.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    data.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        Button(
            onClick = {
                if (pagerState.currentPage < pages.lastIndex) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    onFinished()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        ) {
            Text(if (pagerState.currentPage < pages.lastIndex) "Next" else "Get started")
        }
    }
}
