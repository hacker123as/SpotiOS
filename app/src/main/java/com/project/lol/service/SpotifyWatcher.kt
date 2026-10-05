package com.project.lol.service

import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.project.lol.util.Logger

/**
 * Notices the Spotify app (Server Mode > Start with Spotify). Android has no event for "another
 * app opened", so this watches for Spotify's own notification, which it shows as soon as it
 * has something playing or ready to play. When it appears, the SpotiOS server starts (or, if it
 * is already running, checks right away whether to take over playback for a remembered device).
 * When Spotify's notification goes away for good (the app was closed), a running server is told
 * so it can pause ("Pause when Spotify closes").
 * Needs the user to allow notification access; SpotiOS only looks at Spotify's notifications.
 */
class SpotifyWatcher : NotificationListenerService() {

    private var lastSeenAt = 0L
    private val main = Handler(Looper.getMainLooper())
    private val closedCheck = Runnable {
        // Spotify swaps its notification (local player <-> remote control): only a notification
        // that stays gone means the app was closed.
        val still = runCatching { activeNotifications?.any { it.packageName == ServerMode.SPOTIFY_PACKAGE } }.getOrNull()
        if (still == false && ServerMode.isOn(this)) {
            Logger.i(TAG, "Spotify was closed")
            MediaNotificationService.instance?.onSpotifyClosed()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Logger.i(TAG, "listening for Spotify")
        val spotify = runCatching { activeNotifications?.any { it.packageName == ServerMode.SPOTIFY_PACKAGE } }.getOrNull() == true
        if (spotify) seen()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.packageName != ServerMode.SPOTIFY_PACKAGE) return
        main.removeCallbacks(closedCheck)
        seen()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn?.packageName != ServerMode.SPOTIFY_PACKAGE) return
        main.removeCallbacks(closedCheck)
        main.postDelayed(closedCheck, 2_500L)
    }

    override fun onListenerDisconnected() {
        main.removeCallbacks(closedCheck)
        super.onListenerDisconnected()
    }

    private fun seen() {
        if (!ServerMode.isOn(this)) return
        // A running server always gets told (to take the music over from the default device);
        // starting a stopped one is what "Start with Spotify" is for.
        if (MediaNotificationService.instance == null && !ServerMode.startWithSpotify(this)) return
        // Spotify updates its notification on every play, pause and song change; react at
        // most every 8 s.
        val now = System.currentTimeMillis()
        if (now - lastSeenAt < 8_000) return
        lastSeenAt = now
        Logger.i(TAG, "Spotify is active")
        ServerMode.startInBackground(this, "spotify")
    }

    private companion object {
        const val TAG = "spotify-watch"
    }
}
