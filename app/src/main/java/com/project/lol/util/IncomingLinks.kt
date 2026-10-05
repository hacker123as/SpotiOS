package com.project.lol.util

import android.content.Intent
import android.net.Uri

/**
 * Works out what an incoming intent wants SpotiOS to open: a Spotify link (web link,
 * spotify: URI or shared text) or a user script to install (.user.js link, a Greasy Fork
 * or OpenUserJS script page, or a .js file).
 */
object IncomingLinks {

    private val SPOTIFY_HOSTS = setOf("spotify.link", "spotify.app.link")
    private val URL_RX = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val URI_RX = Regex("""spotify:[A-Za-z0-9:_%.\-]+""")
    private val TYPES = setOf("track", "album", "artist", "playlist", "show", "episode", "audiobook", "chapter", "user", "genre", "concert", "prerelease")

    /** An https URL SpotiOS can load for a Spotify link in [intent], or null. */
    fun spotifyUrl(intent: Intent?): String? {
        intent ?: return null
        intent.data?.let { uri -> fromUri(uri.toString())?.let { return it } }
        if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
            URL_RX.findAll(text).forEach { m -> fromUri(m.value.trimEnd('.', ',', ')', '!'))?.let { return it } }
            URI_RX.find(text)?.let { m -> fromUri(m.value)?.let { return it } }
        }
        return null
    }

    private fun fromUri(raw: String): String? {
        if (raw.startsWith("spotify:", ignoreCase = true)) return fromSpotifyUri(raw)
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "https" && scheme != "http") return null
        val host = uri.host?.lowercase() ?: return null
        if (host in SPOTIFY_HOSTS) return raw.replaceFirst(Regex("^http:", RegexOption.IGNORE_CASE), "https:")
        if (host == "play.spotify.com") return uri.buildUpon().scheme("https").authority("open.spotify.com").build().toString()
        if (host == "spotify.com" || host.endsWith(".spotify.com")) {
            if (host == "accounts.spotify.com") return null
            return raw.replaceFirst(Regex("^http:", RegexOption.IGNORE_CASE), "https:")
        }
        return null
    }

    /** spotify:track:ID, spotify:user:x:playlist:ID, spotify:search:q ... as an open.spotify.com URL. */
    private fun fromSpotifyUri(raw: String): String {
        val parts = raw.split(':').drop(1).map { Uri.decode(it) }
        val base = "https://open.spotify.com"
        if (parts.isEmpty()) return "$base/"
        if (parts[0] == "search") return "$base/search/" + Uri.encode(parts.drop(1).joinToString(":"))
        if (parts.contains("collection")) return "$base/collection/tracks"
        // Use the last type:id pair, so spotify:user:name:playlist:ID opens the playlist.
        for (i in parts.size - 2 downTo 0) {
            val type = parts[i].lowercase()
            if (type in TYPES && parts[i + 1].isNotBlank()) return "$base/$type/" + Uri.encode(parts[i + 1])
        }
        return "$base/"
    }

    /** Where to fetch a user script from: an http(s) URL, or a content:/file: URI of a .js file. */
    fun userScriptSource(intent: Intent?): String? {
        intent ?: return null
        intent.data?.let { uri -> scriptFrom(uri, intent.type)?.let { return it } }
        if (intent.action == Intent.ACTION_SEND) {
            @Suppress("DEPRECATION")
            (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { s -> scriptFrom(s, intent.type)?.let { return it } }
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
            URL_RX.findAll(text).forEach { m ->
                val u = runCatching { Uri.parse(m.value.trimEnd('.', ',', ')', '!')) }.getOrNull() ?: return@forEach
                scriptFrom(u, null)?.let { return it }
            }
        }
        return null
    }

    private fun scriptFrom(uri: Uri, type: String?): String? {
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme == "content" || scheme == "file") {
            val name = uri.lastPathSegment.orEmpty().lowercase()
            val js = type?.contains("javascript") == true || name.endsWith(".js")
            return if (js) uri.toString() else null
        }
        if (scheme != "https" && scheme != "http") return null
        val path = uri.path.orEmpty()
        if (path.lowercase().endsWith(".user.js")) return uri.toString()
        val host = uri.host?.lowercase().orEmpty()
        // A Greasy Fork / Sleazy Fork script page: /en/scripts/12345-name -> its install link.
        if (host.endsWith("greasyfork.org") || host.endsWith("sleazyfork.org")) {
            val id = Regex("""/scripts/(\d+)""").find(path)?.groupValues?.get(1) ?: return null
            val site = if (host.endsWith("sleazyfork.org")) "sleazyfork.org" else "greasyfork.org"
            return "https://$site/scripts/$id/code/script.user.js"
        }
        // OpenUserJS: /scripts/<user>/<name> -> /install/<user>/<name>.user.js
        if (host.endsWith("openuserjs.org")) {
            val m = Regex("""^/scripts/([^/]+)/([^/?#]+)""").find(path) ?: return null
            return "https://openuserjs.org/install/${m.groupValues[1]}/${m.groupValues[2]}.user.js"
        }
        return null
    }
}
