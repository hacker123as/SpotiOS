package com.project.lol.bridge

import java.util.Locale

/** Where AndBridge's native requests (no CORS, Spotify's cookies attached) may go. Plain JVM, so it's unit tested. */
object NativeNet {
    private val HOST_SUFFIXES = listOf("spotify.com", "scdn.co", "spotifycdn.com")

    /** https, no user-info, and one of Spotify's hosts (spotify.com, scdn.co, spotifycdn.com or a subdomain). */
    fun allowed(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return false
        if (!"https".equals(uri.scheme, ignoreCase = true)) return false
        if (uri.rawUserInfo != null) return false
        val host = uri.host?.lowercase(Locale.ROOT)?.trimEnd('.') ?: return false
        return HOST_SUFFIXES.any { host == it || host.endsWith(".$it") }
    }
}
