package com.skillmcp.mentor.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class AppLanguageTest {
    private val context: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun systemAndBlankFollowTheDevice() {
        assertEquals("", localeTagsFor(""))
        assertEquals("", localeTagsFor("system"))
        assertEquals("te", localeTagsFor("te"))
    }

    @Test
    fun mirrorIsEmptyUntilWrittenThenSurvivesReads() {
        assertEquals("", AppLanguageMirror.read(context))
        AppLanguageMirror.write(context, "hi")
        assertEquals("hi", AppLanguageMirror.read(context))
        AppLanguageMirror.write(context, "system")
        assertEquals("system", AppLanguageMirror.read(context))
    }
}
