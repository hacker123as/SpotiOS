package com.project.lol.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.project.lol.util.Logger

/**
 * Notices the Spotify app (Server Mode > Start with Spotify). Android has no event for "another
 * app opened", so this watches for Spotify's own notification, which it shows as soon as it
 * has something playing or ready to play. When it appears, the SpotiOS server starts (or, if it
 * is already running, checks right away whether to take over playback for a remembered device).
 * Needs the user to allow notification access; SpotiOS only looks at Spotify's notifications.
 */
class SpotifyWatcher : NotificationListenerService() {

    private var lastSeenAt = 0L

    override fun onListenerConnected() {
        super.onListenerConnected()
        Logger.i(TAG, "listening for Spotify")
        val spotify = runCatching { activeNotifications?.any { it.packageName == ServerMode.SPOTIFY_PACKAGE } }.getOrNull() == true
        if (spotify) seen()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.packageName != ServerMode.SPOTIFY_PACKAGE) return
        seen()
    }

    private fun seen() {
        if (!ServerMode.isOn(this) || !ServerMode.startWithSpotify(this)) return
        // Spotify updates its notification every few seconds while playing; react to the
        // first one and then at most every 20 s.
        val now = System.currentTimeMillis()
        if (now - lastSeenAt < 20_000) return
        lastSeenAt = now
        Logger.i(TAG, "Spotify is active")
        ServerMode.startInBackground(this, "spotify")
    }

    private companion object {
        const val TAG = "spotify-watch"
    }
}
