package com.skillmcp.mentor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.skillmcp.mentor.llm.UsageByDayRow
import java.util.Locale

private const val INR_PER_USD_ESTIMATE = 83.0

@Composable
fun UsageSpendBarChart(
    days: List<UsageByDayRow>,
    showInr: Boolean,
    modifier: Modifier = Modifier,
) {
    val series = days.take(14).reversed()
    if (series.isEmpty()) return
    val maxUsd = series.maxOf { it.estimatedUsd }.coerceAtLeast(0.0001)
    var selectedIndex by remember(series) { mutableIntStateOf(series.lastIndex) }
    val barColor = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val selected = series.getOrNull(selectedIndex)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .semantics {
                        contentDescription =
                            "Daily spend chart, ${series.size} days. Tap a bar for details."
                    }
                    .pointerInput(series) {
                        detectTapGestures { offset ->
                            val barWidth = size.width / (series.size * 1.4f)
                            val index = (offset.x / (barWidth * 1.4f)).toInt().coerceIn(0, series.lastIndex)
                            selectedIndex = index
                        }
                    },
        ) {
            val barWidth = size.width / (series.size * 1.4f)
            series.forEachIndexed { index, row ->
                val fraction = (row.estimatedUsd / maxUsd).toFloat().coerceIn(0.05f, 1f)
                val left = index * barWidth * 1.4f + barWidth * 0.2f
                val barHeight = (size.height - 24f) * fraction
                val color = if (index == selectedIndex) barColor else barColor.copy(alpha = 0.55f)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, size.height - 24f - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }
        }
        Text(
            series.map { shortDayLabel(it.dayKey) }.joinToString("  "),
            style = MaterialTheme.typography.labelSmall,
            color = muted,
            modifier = Modifier.padding(top = 4.dp),
        )
        selected?.let { row ->
            val usd = String.format(Locale.US, "%.4f", row.estimatedUsd)
            val detail =
                if (showInr) {
                    val inr = row.estimatedUsd * INR_PER_USD_ESTIMATE
                    "${row.dayKey}: $${usd} (~₹${String.format(Locale.US, "%.0f", inr)} est.) · ${row.requestCount} requests"
                } else {
                    "${row.dayKey}: $$usd · ${row.requestCount} requests"
                }
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (showInr) {
            Text(
                "INR amounts use an approximate rate for display only.",
                style = MaterialTheme.typography.labelSmall,
                color = muted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private fun shortDayLabel(dayKey: String): String {
    val parts = dayKey.split("-")
    return if (parts.size >= 3) "${parts[1]}/${parts[2]}" else dayKey.takeLast(5)
}
