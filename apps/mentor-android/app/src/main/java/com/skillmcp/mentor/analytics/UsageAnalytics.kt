package com.skillmcp.mentor.analytics

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.analyticsStore by preferencesDataStore("usage_analytics")

/**
 * Privacy-safe, on-device tallies for Discover ranking. No network upload.
 */
class UsageAnalytics(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var optInCache: Boolean? = null

    fun record(event: String, id: String) {
        scope.launch {
            if (!isOptIn()) return@launch
            mutex.withLock {
                appContext.analyticsStore.edit { prefs ->
                    val key = eventKey(event, id)
                    val current = (prefs[key] as? String)?.toIntOrNull() ?: 0
                    prefs[key] = (current + 1).toString()
                }
            }
        }
    }

    suspend fun topIds(event: String, limit: Int = 10): List<Pair<String, Int>> {
        val prefix = "$event:"
        return appContext.analyticsStore.data.first().asMap()
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
        optInCache = enabled
        scope.launch {
            appContext.analyticsStore.edit { it[KEY_OPT_IN] = enabled.toString() }
        }
    }

    suspend fun isOptIn(): Boolean {
        optInCache?.let { return it }
        return appContext.analyticsStore.data.first()[KEY_OPT_IN]?.toBooleanStrictOrNull() ?: false
    }

    fun isOptInCached(): Boolean = optInCache ?: false

    private fun eventKey(event: String, id: String) = stringPreferencesKey("$event:$id")

    companion object {
        const val EVENT_USE_CASE = "use_case"
        const val EVENT_SKILL = "skill"
        private val KEY_OPT_IN = stringPreferencesKey("opt_in")
    }
}
