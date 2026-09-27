package com.skillmcp.mentor.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SpendFormatTest {
    @Test
    fun formatsTinyAmounts() {
        assertEquals("<\$0.01", SpendFormat.formatUsdEstimate(0.0001))
    }
}
