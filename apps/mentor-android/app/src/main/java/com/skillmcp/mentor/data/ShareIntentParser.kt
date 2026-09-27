package com.skillmcp.mentor.data

/**
 * Parses what another app shared with Lumina (ACTION_SEND / ACTION_SEND_MULTIPLE). Pure: callers pass
 * the intent fields as strings, so the rules are unit-tested without Android.
 */
object ShareIntentParser {
    const val ACTION_SEND = "android.intent.action.SEND"
    const val ACTION_SEND_MULTIPLE = "android.intent.action.SEND_MULTIPLE"
    const val MAX_TEXT_CHARS = 20_000

    data class Shared(
        /** Text for the composer (may be empty when only an image was shared). */
        val text: String,
        /** content:// URI of a shared image, if any. */
        val imageUri: String? = null,
        val isLink: Boolean = false,
    )

    /**
     * @param linkPrompt formats a bare shared link into a request, e.g. "Summarize this page: %s".
     * @param streamUris EXTRA_STREAM values (first image wins for SEND_MULTIPLE).
     */
    fun parse(
        action: String?,
        mimeType: String?,
        text: String?,
        subject: String?,
        streamUris: List<String?>,
        linkPrompt: (String) -> String = { it },
    ): Shared? {
        if (action != ACTION_SEND && action != ACTION_SEND_MULTIPLE) return null
        val type = mimeType?.lowercase().orEmpty()
        val body = text?.trim().orEmpty().take(MAX_TEXT_CHARS)
        val title = subject?.trim().orEmpty()
        val image =
            if (type.startsWith("image/")) {
                // Only content:// URIs: file:// would let another app point us at our own private files.
                streamUris.firstOrNull { it != null && it.startsWith("content://") }
            } else {
                null
            }
        if (image == null && !type.startsWith("text/") && body.isEmpty()) return null
        val isLink = body.isNotEmpty() && !body.contains(' ') && !body.contains('\n') &&
            (body.startsWith("https://") || body.startsWith("http://"))
        val composed =
            when {
                isLink -> linkPrompt(body)
                title.isNotEmpty() && body.isNotEmpty() && !body.contains(title) -> "$title\n\n$body"
                body.isNotEmpty() -> body
                else -> ""
            }
        if (composed.isEmpty() && image == null) return null
        return Shared(text = composed, imageUri = image, isLink = isLink)
    }
}
