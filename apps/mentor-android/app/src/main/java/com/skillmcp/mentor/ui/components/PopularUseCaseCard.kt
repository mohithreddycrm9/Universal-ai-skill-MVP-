package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.mentor.PopularUseCase

@Composable
fun PopularUseCaseCard(
    useCase: PopularUseCase,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(onClick = onClick, modifier = modifier.width(260.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(
                useCase.category.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(useCase.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                useCase.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
