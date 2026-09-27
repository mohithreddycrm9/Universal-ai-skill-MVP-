package com.skillmcp.mentor.util

import kotlin.test.Test
import kotlin.test.assertTrue
import java.net.UnknownHostException

class UserFacingErrorsTest {
    @Test
    fun mapsOfflineErrors() {
        val msg = UserFacingErrors.message(UnknownHostException())
        assertTrue(msg.contains("network", ignoreCase = true))
    }

    @Test
    fun mapsAuthErrors() {
        val msg = UserFacingErrors.message(IllegalStateException("HTTP 401 unauthorized"))
        assertTrue(msg.contains("Authentication", ignoreCase = true))
    }
}
