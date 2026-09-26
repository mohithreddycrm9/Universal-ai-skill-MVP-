package com.skillmcp.mentor.analytics

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.analyticsStore by preferencesDataStore("usage_analytics")

/**
 * Privacy-safe, on-device tallies for Discover ranking. No network upload.
 */
class UsageAnalytics(private val context: Context) {
    fun record(event: String, id: String) {
        if (!isOptIn()) return
        val key = eventKey(event, id)
        runBlocking {
            context.analyticsStore.edit { prefs ->
                val current = prefs[key]?.toIntOrNull() ?: 0
                prefs[key] = (current + 1).toString()
            }
        }
    }

    fun topIds(event: String, limit: Int = 10): List<Pair<String, Int>> =
        runBlocking {
            val prefix = "$event:"
            context.analyticsStore.data.first().asMap()
                .mapNotNull { (k, v) ->
                    val name = k.name
                    if (!name.startsWith(prefix)) return@mapNotNull null
                    val id = name.removePrefix(prefix)
                    val count = (v as? String)?.toIntOrNull() ?: 0
                    id to count
                }
                .sortedByDescending { it.second }
                .take(limit)
        }

    fun setOptIn(enabled: Boolean) {
        runBlocking {
            context.analyticsStore.edit { it[KEY_OPT_IN] = enabled.toString() }
        }
    }

    fun isOptIn(): Boolean =
        runBlocking {
            context.analyticsStore.data.first()[KEY_OPT_IN]?.toBooleanStrictOrNull() ?: false
        }

    private fun eventKey(event: String, id: String) = stringPreferencesKey("$event:$id")

    companion object {
        const val EVENT_USE_CASE = "use_case"
        const val EVENT_SKILL = "skill"
        private val KEY_OPT_IN = stringPreferencesKey("opt_in")
    }
}
