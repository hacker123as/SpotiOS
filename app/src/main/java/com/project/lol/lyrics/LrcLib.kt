package com.project.lol.lyrics

import com.project.lol.util.Logger
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

/**
 * Lyrics lookup against LRCLIB (https://lrclib.net), an open, free lyrics
 * database with synced (LRC) lyrics. Used for songs Spotify has no lyrics for.
 */
object LrcLib {

    private const val TAG = "LrcLib"
    private const val BASE = "https://lrclib.net/api"
    private const val UA = "SpotiOS (https://github.com/hacker123as/SpotiOS)"
    private const val MAX_CACHE = 64

    private val cache = object : LinkedHashMap<String, String>(MAX_CACHE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > MAX_CACHE
    }

    /**
     * Returns a JSON object string {synced, plain, instrumental}, or "null"
     * when nothing was found. Blocking: call off the main thread.
     */
    fun lookup(artist: String, title: String, durationSec: Double): String {
        val key = "${artist.lowercase()}\u0001${title.lowercase()}"
        synchronized(cache) { cache[key]?.let { return it } }

        val artists = artist.split(", ", " & ", " feat. ", " ft. ")
            .map { it.trim() }.filter { it.isNotEmpty() }
        val primary = artists.firstOrNull() ?: artist
        val cleanTitle = cleanTitle(title)

        var failed = false
        val hit = runCatching {
            get(primary, title, durationSec)
                ?: (if (cleanTitle != title) get(primary, cleanTitle, durationSec) else null)
                ?: search(primary, cleanTitle, durationSec)
        }.onFailure { failed = true; Logger.e(TAG, "lookup failed: ${it.message}") }.getOrNull()

        val result = hit?.let {
            JSONObject().apply {
                put("synced", it.optString("syncedLyrics").takeIf { s -> s.isNotBlank() && s != "null" } ?: JSONObject.NULL)
                put("plain", it.optString("plainLyrics").takeIf { s -> s.isNotBlank() && s != "null" } ?: JSONObject.NULL)
                put("instrumental", it.optBoolean("instrumental", false))
            }.toString()
        } ?: "null"

        // Only cache real answers; a network error should be retried next time.
        if (!failed) synchronized(cache) { cache[key] = result }
        return result
    }

    /** "Song - Remastered 2011" / "Song (feat. X)" -> "Song" */
    private fun cleanTitle(t: String): String =
        t.replace(Regex("""\s*[(\[](feat\.?|ft\.?|with)\s[^)\]]*[)\]]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+-\s+.*(remaster|version|edit|live|mix|mono|stereo).*$""", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifEmpty { t }

    private fun get(artist: String, title: String, durationSec: Double): JSONObject? {
        val q = buildString {
            append("artist_name=").append(enc(artist))
            append("&track_name=").append(enc(title))
            if (durationSec > 0) append("&duration=").append(durationSec.toInt())
        }
        val body = http("$BASE/get?$q") ?: return null
        return JSONObject(body).takeIf { hasLyrics(it) }
    }

    private fun search(artist: String, title: String, durationSec: Double): JSONObject? {
        val body = http("$BASE/search?track_name=${enc(title)}&artist_name=${enc(artist)}") ?: return null
        val arr = JSONArray(body)
        var best: JSONObject? = null
        var bestScore = Double.MAX_VALUE
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (!hasLyrics(o)) continue
            val d = o.optDouble("duration", 0.0)
            // Prefer synced lyrics, then the closest duration.
            var score = if (durationSec > 0 && d > 0) abs(d - durationSec) else 30.0
            if (o.optString("syncedLyrics").isBlank() || o.isNull("syncedLyrics")) score += 20
            if (durationSec > 0 && d > 0 && abs(d - durationSec) > 15) continue
            if (score < bestScore) { bestScore = score; best = o }
        }
        return best
    }

    private fun hasLyrics(o: JSONObject): Boolean =
        o.optBoolean("instrumental", false) ||
            (!o.isNull("syncedLyrics") && o.optString("syncedLyrics").isNotBlank()) ||
            (!o.isNull("plainLyrics") && o.optString("plainLyrics").isNotBlank())

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** Body on 200, null on 404; throws on network errors. */
    private fun http(url: String): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", UA)
            conn.setRequestProperty("Lrclib-Client", UA)
            when (conn.responseCode) {
                200 -> conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                404 -> null
                else -> throw IllegalStateException("HTTP ${conn.responseCode}")
            }
        } finally {
            conn.disconnect()
        }
    }
}
