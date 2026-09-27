package com.skillmcp.mentor.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat

/** "" / "system" follow the device language; anything else is a BCP-47 tag such as "hi". */
internal fun localeTagsFor(languageTag: String): String =
    if (languageTag.isBlank() || languageTag == "system") "" else languageTag

fun applyAppLanguage(languageTag: String) {
    val desired = localeTagsFor(languageTag)
    // Re-applying the same locales would recreate running activities for nothing.
    if (AppCompatDelegate.getApplicationLocales().toLanguageTags() == desired) return
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(desired))
}

/**
 * Tiny synchronous mirror of the language preference. The main preferences are async (DataStore),
 * so without this the first frame after a cold start would draw in the device language and then
 * flash when the saved language arrives. Reading one small SharedPreferences key in
 * Application.onCreate is cheap and lets us apply the language before any Activity exists.
 */
object AppLanguageMirror {
    private const val FILE = "app_language_mirror"
    private const val KEY = "tag"

    fun read(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, "") ?: ""

    fun write(context: Context, languageTag: String) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (prefs.getString(KEY, "") != languageTag) prefs.edit { putString(KEY, languageTag) }
    }
}
