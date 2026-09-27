package com.skillmcp.mentor.data

import com.skillmcp.mentor.data.ShareIntentParser.ACTION_SEND
import com.skillmcp.mentor.data.ShareIntentParser.ACTION_SEND_MULTIPLE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareIntentParserTest {
    private fun parse(
        action: String? = ACTION_SEND,
        type: String? = "text/plain",
        text: String? = null,
        subject: String? = null,
        streams: List<String?> = emptyList(),
    ) = ShareIntentParser.parse(action, type, text, subject, streams) { "Summarize: $it" }

    @Test
    fun plainTextIsTrimmedIntoTheComposer() {
        assertEquals(ShareIntentParser.Shared("Meeting notes"), parse(text = "  Meeting notes \n"))
    }

    @Test
    fun bareLinkBecomesASummaryRequest() {
        val shared = parse(text = "https://example.org/a")!!
        assertTrue(shared.isLink)
        assertEquals("Summarize: https://example.org/a", shared.text)
    }

    @Test
    fun subjectIsPrependedUnlessAlreadyInTheText() {
        assertEquals("Trip\n\nPack light", parse(text = "Pack light", subject = "Trip")!!.text)
        assertEquals("Trip: pack light", parse(text = "Trip: pack light", subject = "Trip")!!.text)
    }

    @Test
    fun sharedImageUsesOnlyContentUris() {
        val shared = parse(type = "image/jpeg", streams = listOf("content://photos/1"))!!
        assertEquals("content://photos/1", shared.imageUri)
        assertEquals("", shared.text)
        assertNull(parse(type = "image/png", streams = listOf("file:///data/data/com.skillmcp.mentor/x")))
    }

    @Test
    fun multipleImagesTakeTheFirstContentUriAndCaption() {
        val shared = parse(ACTION_SEND_MULTIPLE, "image/*", "Which is nicer?", streams = listOf(null, "content://a", "content://b"))!!
        assertEquals("content://a", shared.imageUri)
        assertEquals("Which is nicer?", shared.text)
    }

    @Test
    fun unsupportedIntentsAreIgnored() {
        assertNull(parse(action = "android.intent.action.VIEW", text = "hi"))
        assertNull(parse(text = "   "))
        assertNull(parse(type = "application/zip", streams = listOf("content://z")))
    }

    @Test
    fun hugeTextIsCapped() {
        assertEquals(ShareIntentParser.MAX_TEXT_CHARS, parse(text = "a".repeat(50_000))!!.text.length)
    }
}
