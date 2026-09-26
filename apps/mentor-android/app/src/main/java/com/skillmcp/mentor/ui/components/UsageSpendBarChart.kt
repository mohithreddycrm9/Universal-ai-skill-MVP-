package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.llm.UsageByDayRow

@Composable
fun UsageSpendBarChart(
    days: List<UsageByDayRow>,
    modifier: Modifier = Modifier,
) {
    val series = days.take(30).reversed()
    if (series.isEmpty()) return
    val maxUsd = series.maxOf { it.estimatedUsd }.coerceAtLeast(0.0001)
    val barColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        val barWidth = size.width / (series.size * 1.4f)
        series.forEachIndexed { index, row ->
            val fraction = (row.estimatedUsd / maxUsd).toFloat().coerceIn(0.05f, 1f)
            val left = index * barWidth * 1.4f + barWidth * 0.2f
            val barHeight = size.height * fraction
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f),
            )
        }
    }
}
