package com.skillmcp.mentor.util

import java.util.Locale

object SpendFormat {
    fun formatUsdEstimate(usd: Double): String =
        when {
            usd <= 0 -> ""
            usd < 0.01 -> "<\$0.01"
            else -> "\$${String.format(Locale.US, "%.2f", usd)}"
        }
}
