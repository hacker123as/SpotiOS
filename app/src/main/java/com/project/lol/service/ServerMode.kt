package com.project.lol.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.project.lol.R
import com.project.lol.ui.onboarding.isIgnoringBatteryOptimizations
import com.project.lol.util.Logger
import org.json.JSONObject

/**
 * SpotiOS Server Mode: SpotiOS stays running in the background as a Spotify Connect device,
 * so the normal Spotify app (or a laptop, or anything with Spotify) can pick "SpotiOS" from its
 * device list and play through it. The player outlives the screen (see PlayerHost), the media
 * service keeps the connection to Spotify alive, and the page shows a now-playing screen
 * instead of the whole web player.
 */
object ServerMode {

    private const val TAG = "server"
    const val KEY = "SpoServer"
    /** Start the server when the Spotify app shows its notification (needs notification access). */
    const val KEY_WITH_SPOTIFY = "SpoServerWithSpotify"
    /** Set once the user picked Server or Normal (welcome screen, or once after updating). */
    const val KEY_MODE_ASKED = "ModeAsked"
    const val SPOTIFY_PACKAGE = "com.spotify.music"

    private const val PREFS = "spotilol_prefs"
    private const val NOTICE_CHANNEL = "spotios_server_notice"
    private const val NOTICE_ID = 7

    fun isOn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun setOn(context: Context, on: Boolean) {
        Logger.i(TAG, "server mode -> $on")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY, on)
            .putBoolean(KEY_MODE_ASKED, true)
            .apply()
        if (on) ServerWatchdog.schedule(context) else {
            cancelNotice(context)
            ServerWatchdog.cancel(context)
        }
    }

    fun startWithSpotify(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_WITH_SPOTIFY, false)

    fun setStartWithSpotify(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_WITH_SPOTIFY, on).apply()
    }

    fun hasNotificationAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** The system page where SpotiOS can be allowed to see notifications (to notice Spotify opening). */
    fun notificationAccessIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, SpotifyWatcher::class.java).flattenToString()
                )
            if (detail.resolveActivity(context.packageManager) != null) return detail
        }
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    fun isLoggedIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("LoggedIn", false)

    /**
     * Starts the server without opening SpotiOS (Spotify just opened, or the server was asked
     * to start from a notification). Android only lets an app start this from the background
     * when battery optimization is off for it; otherwise a "Tap to start" notification is shown.
     */
    fun startInBackground(context: Context, reason: String) {
        if (!isOn(context)) return
        val running = MediaNotificationService.instance
        if (running != null) {
            // Same process: tell the running service directly (a service start from the
            // background can be refused).
            if (reason == "spotify") running.onSpotifySeen()
            return
        }
        if (!isLoggedIn(context)) {
            showNotice(context, context.getString(R.string.server_notice_signin))
            return
        }
        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MediaNotificationService::class.java).setAction(MediaNotificationService.ACTION_SERVER_START)
            )
            Logger.i(TAG, "server started in the background ($reason)")
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException when battery optimization is on.
            Logger.w(TAG, "background start blocked ($reason): ${e.javaClass.simpleName}")
            showNotice(context, context.getString(R.string.server_notice_tap_to_start))
        }
    }

    /** A plain notification that opens SpotiOS, which starts the server. */
    fun showNotice(context: Context, text: String) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(NOTICE_CHANNEL, context.getString(R.string.server_notice_channel), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { setShowBadge(false) }
        )
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val pi = PendingIntent.getActivity(context, 7, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, NOTICE_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.server_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(NOTICE_ID, n) }
    }

    fun cancelNotice(context: Context) {
        runCatching { context.getSystemService(NotificationManager::class.java)?.cancel(NOTICE_ID) }
    }

    /** Server Mode settings and phone facts for the Server screen in the page. */
    fun statusJson(context: Context): String {
        val o = JSONObject()
        o.put("on", isOn(context))
        o.put("battery", isIgnoringBatteryOptimizations(context))
        o.put("withSpotify", startWithSpotify(context))
        o.put("notifAccess", hasNotificationAccess(context))
        o.put("spotifyInstalled", runCatching { context.packageManager.getPackageInfo(SPOTIFY_PACKAGE, 0); true }.getOrDefault(false))
        o.put("model", Build.MODEL ?: "")
        o.put("phoneName", runCatching { Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) }.getOrNull() ?: "")
        o.put("since", com.project.lol.webview.PlayerHost.createdAt)
        o.put("service", MediaNotificationService.instance != null)
        return o.toString()
    }
}
