package com.project.lol.webview

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.project.lol.util.Logger
import java.util.Locale

/**
 * Which pages may load inside SpotiOS's WebViews. The player page has AndBridge (native calls)
 * on it, so it must only ever show Spotify and the sign-in pages Spotify sends people to.
 */
object SafeNav {

    private val SPOTIFY_SUFFIXES = listOf("spotify.com", "spotify.link", "spotify.app.link")
    /** accounts.google.de, accounts.google.co.uk ... */
    private val GOOGLE_CC = Regex("""^accounts\.google\.(?:[a-z]{2,3}|co\.[a-z]{2}|com\.[a-z]{2})$""")

    private fun hostOf(url: String): String? =
        runCatching { Uri.parse(url).host }.getOrNull()?.lowercase(Locale.ROOT)?.trimEnd('.')

    private fun under(host: String, suffix: String) = host == suffix || host.endsWith(".$suffix")

    fun isSpotify(url: String): Boolean = spotifyHost(hostOf(url))

    fun spotifyHost(host: String?): Boolean = host != null && SPOTIFY_SUFFIXES.any { under(host, it) }

    /** Google, Facebook and Apple sign-in (Spotify's "Continue with ..." buttons). */
    fun isSignIn(url: String): Boolean = signInHost(hostOf(url))

    fun signInHost(host: String?): Boolean {
        host ?: return false
        return under(host, "google.com") || GOOGLE_CC.matches(host) || host == "accounts.youtube.com" ||
            under(host, "facebook.com") || under(host, "apple.com")
    }

    /** The two consent / captcha helpers the sign-in pages use as full pages. */
    private fun helperHost(host: String?): Boolean =
        host != null && (under(host, "recaptcha.net") || under(host, "gstatic.com"))

    fun allowedInPlayer(url: String): Boolean = allowedHost(hostOf(url))

    fun allowedHost(host: String?): Boolean {
        val h = host?.lowercase(Locale.ROOT)?.trimEnd('.')
        return spotifyHost(h) || signInHost(h) || helperHost(h)
    }

    /** Opens [url] in the phone's browser (or the app that handles it). */
    fun openExternal(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { Logger.w("nav", "no app for an external link: ${it.javaClass.simpleName}") }
    }
}
