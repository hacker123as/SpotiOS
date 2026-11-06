package com.project.lol.webview

/**
 * Spotify's page saying "anonymous" once isn't proof the login is gone: a page loaded during a
 * network hiccup or a Spotify auth blip says it too. Dropping the login then stopped Server Mode
 * until SpotiOS was opened again. So the first "logged out" only reloads the page; a second one
 * within [WINDOW_MS] counts.
 */
object LogoutGuard {
    private const val WINDOW_MS = 10 * 60_000L
    @Volatile private var firstOutAt = 0L

    /** True when this "logged out" should end the session, false when the caller should reload once. */
    fun confirm(now: Long = System.currentTimeMillis()): Boolean {
        val first = firstOutAt
        if (first > 0 && now - first < WINDOW_MS) {
            firstOutAt = 0L
            return true
        }
        firstOutAt = now
        return false
    }

    /** The page said "logged in": forget an earlier blip. */
    fun clear() { firstOutAt = 0L }
}
