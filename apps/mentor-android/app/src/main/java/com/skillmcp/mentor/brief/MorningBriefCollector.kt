package com.skillmcp.mentor.brief

import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.skillmcp.mentor.data.MentorPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class MorningBriefBundle(
    val lines: List<String>,
    val chatPrompt: String,
)

class MorningBriefCollector(
    private val context: Context,
    private val http: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build(),
) {
    suspend fun collect(prefs: MentorPrefs): MorningBriefBundle =
        withContext(Dispatchers.IO) {
            val lines = mutableListOf<String>()
            if (prefs.morningBriefTasks) {
                lines += "Tasks: review your top 3 priorities for today."
            }
            if (prefs.morningBriefCalendar) {
                lines += calendarLines()
            }
            if (prefs.morningBriefWeather) {
                lines += weatherLine()
            }
            if (prefs.morningBriefNews) {
                lines += newsHeadlines()
            }
            if (lines.isEmpty()) {
                lines += "Open Settings → Morning brief to choose what to include."
            }
            val body = lines.joinToString("\n")
            val prompt =
                "Morning brief context (device):\n$body\n\n" +
                    "Turn this into a friendly 5-bullet morning brief with one actionable suggestion."
            MorningBriefBundle(lines, prompt)
        }

    private fun calendarLines(): String {
        if (
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return "Calendar: grant Calendar permission in Settings to include events."
        }
        val now = System.currentTimeMillis()
        val end = now + TimeUnit.HOURS.toMillis(24)
        val uri = CalendarContract.Instances.CONTENT_URI
        val projection =
            arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
            )
        val selection = "${CalendarContract.Instances.BEGIN} >= ? AND ${CalendarContract.Instances.BEGIN} < ?"
        val args = arrayOf(now.toString(), end.toString())
        val events = mutableListOf<String>()
        context.contentResolver.query(uri, projection, selection, args, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
            var count = 0
            while (c.moveToNext() && count < 3) {
                val title = c.getString(0) ?: "Event"
                events += title
                count++
            }
        }
        return if (events.isEmpty()) {
            "Calendar: no events in the next 24h."
        } else {
            "Calendar: ${events.joinToString("; ")}"
        }
    }

    private fun weatherLine(): String =
        runCatching {
            val request =
                Request.Builder()
                    .url("https://api.open-meteo.com/v1/forecast?latitude=28.61&longitude=77.21&current_weather=true")
                    .get()
                    .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "Weather: unavailable right now."
                val json = JSONObject(response.body?.string() ?: "{}")
                val current = json.optJSONObject("current_weather")
                val temp = current?.optDouble("temperature")
                val code = current?.optInt("weathercode")
                "Weather (Delhi area): ${temp ?: "?"}°C, code $code — change city in a future update."
            }
        }.getOrElse { "Weather: could not fetch (offline?)." }

    private fun newsHeadlines(): String =
        runCatching {
            val request =
                Request.Builder()
                    .url("https://feeds.bbci.co.uk/news/world/rss.xml")
                    .header("User-Agent", "UniversalAI-Mentor/1.0")
                    .get()
                    .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "News: feed unavailable."
                val xml = response.body?.string() ?: ""
                val titles =
                    Regex("<title><!\\[CDATA\\[(.+?)]]></title>|<title>([^<]+)</title>")
                        .findAll(xml)
                        .map { it.groupValues[1].ifBlank { it.groupValues[2] } }
                        .filter { !it.equals("BBC News", ignoreCase = true) }
                        .take(3)
                        .toList()
                if (titles.isEmpty()) "News: no headlines parsed."
                else "Headlines: ${titles.joinToString(" · ")}"
            }
        }.getOrElse { "News: could not load headlines." }
}
