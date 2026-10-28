package com.project.lol.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Handler
import com.project.lol.util.Logger

/**
 * Notices the moment the Spotify app on this phone comes to the front, before anything plays
 * (Server Mode take-over). SpotifyWatcher only sees Spotify's notification, which Spotify
 * shows once it plays, so opening Spotify alone went unnoticed. Android has no "app opened"
 * broadcast; with usage access, the system's app usage events say which app came to the front.
 * Checked about once a second, and only while the screen is on (nobody opens Spotify with the
 * screen off), so it costs next to nothing.
 */
class SpotifyOpenWatch(
    private val context: Context,
    private val handler: Handler,
    private val onOpen: () -> Unit,
) {
    private var running = false
    private var since = 0L
    private var front = ""
    private var firedAt = 0L

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            try { check() } catch (e: Exception) { Logger.w(TAG, "usage check failed: ${e.javaClass.simpleName}") }
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    val isRunning: Boolean get() = running

    /** Starts watching (screen on). Does nothing without usage access. */
    fun start() {
        if (running) return
        if (!ServerMode.hasUsageAccess(context)) return
        running = true
        // A little look back: Spotify opened right as the screen came on still counts.
        since = System.currentTimeMillis() - 3_000L
        front = ""
        handler.removeCallbacks(tick)
        handler.post(tick)
        Logger.i(TAG, "watching for Spotify opening")
    }

    fun stop() {
        if (!running) return
        running = false
        handler.removeCallbacks(tick)
    }

    private fun check() {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(since, now) ?: return
        since = now
        val e = UsageEvents.Event()
        var opened = false
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            // ACTIVITY_RESUMED (Android 10+) has the same value as the older MOVE_TO_FOREGROUND.
            if (e.eventType != RESUMED) continue
            val pkg = e.packageName ?: continue
            if (pkg == ServerMode.SPOTIFY_PACKAGE && front != ServerMode.SPOTIFY_PACKAGE) opened = true
            front = pkg
        }
        if (!opened || now - firedAt < DEBOUNCE_MS) return
        firedAt = now
        Logger.i(TAG, "Spotify opened")
        onOpen()
    }

    private companion object {
        const val TAG = "spotify-open"
        const val INTERVAL_MS = 1_000L
        const val DEBOUNCE_MS = 4_000L
        /** UsageEvents.Event.ACTIVITY_RESUMED (API 29) and MOVE_TO_FOREGROUND before it. */
        const val RESUMED = 1
    }
}
