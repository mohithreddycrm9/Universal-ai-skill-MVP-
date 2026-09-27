package com.skillmcp.mentor.ui.components.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.mentor.PopularUseCase
import com.skillmcp.mentor.ui.theme.BrandColors
import com.skillmcp.mentor.ui.theme.MentorDimens

@Composable
fun DiscoverHeroCard(
    workflow: PopularUseCase?,
    onTry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(BrandColors.Indigo, BrandColors.IndigoDark),
                ),
                shape = MaterialTheme.shapes.large,
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Workflow of the day",
            style = MaterialTheme.typography.labelLarge,
            color = BrandColors.Coral,
        )
        Text(
            workflow?.title ?: "Plan your week",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Text(
            workflow?.subtitle ?: "Pick a starter and open chat with one tap.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
        )
        Button(
            onClick = onTry,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = BrandColors.Coral,
                    contentColor = Color(0xFF1E1B4B),
                ),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text("Try")
        }
    }
}

@Composable
fun DiscoverSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("What do you want to do?") },
        shape = MaterialTheme.shapes.large,
        singleLine = true,
    )
}
