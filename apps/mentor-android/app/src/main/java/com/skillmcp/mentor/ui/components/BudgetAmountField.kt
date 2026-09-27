package com.skillmcp.mentor.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Keeps free-form decimal typing without resetting the field on every prefs emission.
 */
@Composable
fun BudgetAmountField(
    amountUsd: Double,
    onAmountCommitted: (Double) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    LaunchedEffect(amountUsd) {
        val formatted = if (amountUsd > 0) trimTrailingZeros(amountUsd) else ""
        if (text.isEmpty() || text.toDoubleOrNull() == amountUsd) {
            text = formatted
        }
    }
    OutlinedTextField(
        modifier = modifier,
        value = text,
        onValueChange = { raw ->
            val cleaned = raw.filter { it.isDigit() || it == '.' }
            text = cleaned
            if (cleaned.isEmpty()) {
                onAmountCommitted(0.0)
            } else if (cleaned == "." || cleaned.endsWith(".")) {
                // Allow partial decimals like "0." without snapping to 0
            } else {
                cleaned.toDoubleOrNull()?.let(onAmountCommitted)
            }
        },
        label = { androidx.compose.material3.Text(label) },
        singleLine = true,
    )
}

private fun trimTrailingZeros(value: Double): String {
    val s = value.toString()
    return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
}
