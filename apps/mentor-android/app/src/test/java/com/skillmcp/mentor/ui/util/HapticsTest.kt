package com.skillmcp.mentor.ui.util

import android.view.HapticFeedbackConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class HapticsTest {
    @Test
    fun confirmOnAndroid11AndNewer() {
        assertEquals(HapticFeedbackConstants.CONFIRM, sendHapticConstant(30))
        assertEquals(HapticFeedbackConstants.CONFIRM, sendHapticConstant(35))
    }

    @Test
    fun virtualKeyFallbackBelowAndroid11() {
        assertEquals(HapticFeedbackConstants.VIRTUAL_KEY, sendHapticConstant(26))
        assertEquals(HapticFeedbackConstants.VIRTUAL_KEY, sendHapticConstant(29))
    }
}
