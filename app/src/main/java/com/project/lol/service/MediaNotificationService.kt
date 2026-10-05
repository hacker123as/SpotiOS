package com.project.lol.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.webkit.WebView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.project.lol.R
import androidx.media.MediaBrowserServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import com.project.lol.webview.PlayerHost
import com.project.lol.webview.helpers.AccentTheme
import com.project.lol.util.Logger
import com.project.lol.widget.WidgetSource
import com.project.lol.widget.WidgetState
import com.project.lol.widget.WidgetUpdater
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MediaNotificationService : MediaBrowserServiceCompat() {

    companion object {
        private const val TAG = "MediaNotifService"
        private const val CHANNEL_ID = "spotilol_media_playback"
        private const val NOTIFICATION_ID = 1
        private val mainHandler = Handler(Looper.getMainLooper())
        private const val MEDIA_ID_ROOT = "__ROOT__"

        const val ACTION_PLAY_PAUSE = "com.project.lol.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.project.lol.ACTION_NEXT"
        const val ACTION_PREV = "com.project.lol.ACTION_PREV"
        const val ACTION_SHUFFLE = "com.project.lol.ACTION_SHUFFLE"
        const val ACTION_REPEAT = "com.project.lol.ACTION_REPEAT"
        const val ACTION_FAVORITE = "com.project.lol.ACTION_FAVORITE"
        const val ACTION_WIDGET_REFRESH = "com.project.lol.ACTION_WIDGET_REFRESH"
        /** Server Mode: turn it off (notification action). */
        const val ACTION_SERVER_OFF = "com.project.lol.ACTION_SERVER_OFF"
        /** Server Mode: start the player without a screen (Spotify opened, or a notification). */
        const val ACTION_SERVER_START = "com.project.lol.ACTION_SERVER_START"
        /** Server Mode: the Spotify app just showed up; check auto-connect now. */
        const val ACTION_SPOTIFY_SEEN = "com.project.lol.ACTION_SPOTIFY_SEEN"
        /** How often the Spotify Connect connection is checked and pinged. */
        private const val KEEPALIVE_MS = 20_000L
        /** The last connection report from the page (window.spoKeepAlive), for the Server screen. */
        @Volatile var connReport: String = ""
            private set

        private const val CUSTOM_ACTION_TOGGLE_FAV = "toggle_fav"
        private const val CUSTOM_ACTION_TOGGLE_SHUFFLE = "toggle_shuffle"
        private const val CUSTOM_ACTION_REPEAT = "toggle_repeat"

        private val pendingCallbacks =
            ConcurrentHashMap<String, Result<MutableList<MediaBrowserCompat.MediaItem>>>()
        private val pendingSearchCallbacks =
            ConcurrentHashMap<String, Result<MutableList<MediaBrowserCompat.MediaItem>>>()

        private const val MEDIA_ID_PLAYLISTS = "playlists"
        private const val MEDIA_ID_ALBUMS = "albums"
        private const val MEDIA_ID_ARTISTS = "artists"
        private const val MEDIA_ID_PODCASTS = "podcasts"

        private val PLAYBACK_ACTIONS: Long =
            PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_SEEK_TO

        private const val NOTIF_COLOR = 0xFFE0E0E0.toInt()

        var webView: WebView? = null
        var instance: MediaNotificationService? = null
        private var appContext: Context? = null

        @Volatile
        private var taskRemoved = false

        @JvmStatic
        fun onMediaItemsLoaded(parentId: String, json: String) {
            val result = pendingCallbacks.remove(parentId) ?: return
            val items = mutableListOf<MediaBrowserCompat.MediaItem>()
            if (json.isNotEmpty() && json != "null" && json != "[]") {
                try {
                    val jsonArray = org.json.JSONArray(json)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val name = obj.optString("name", appContext?.getString(R.string.notif_unknown_name) ?: "")
                        val id = obj.optString("id")
                        if (id.isEmpty()) continue
                        val image = obj.optString("image")
                        val artists = obj.optJSONArray("artists")
                        val isBrowsable = obj.optBoolean("browsable", false)
                        var sub = ""
                        if (artists != null && artists.length() > 0) {
                            val artistNames = (0 until artists.length()).map { artists.getString(it) }.joinToString(", ")
                            sub = appContext?.getString(R.string.notif_by_artist, artistNames) ?: artistNames
                        }
                        val desc = MediaDescriptionCompat.Builder()
                            .setMediaId(id)
                            .setTitle(name)
                            .setSubtitle(sub)
                            .setIconUri(if (image.isNotEmpty()) Uri.parse(image) else null)
                            .build()
                        val flags = if (isBrowsable) MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                        else MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                        items.add(MediaBrowserCompat.MediaItem(desc, flags))
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Error parsing media items", e)
                }
            }
            val finalItems = items
            Handler(Looper.getMainLooper()).post { result.sendResult(finalItems) }
        }

        @JvmStatic
        fun onSearchCompleted(query: String, json: String) {
            val result = pendingSearchCallbacks.remove(query) ?: return
            val items = mutableListOf<MediaBrowserCompat.MediaItem>()
            if (json.isNotEmpty() && json != "null" && json != "[]") {
                try {
                    val jsonArray = org.json.JSONArray(json)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val name = obj.optString("name", appContext?.getString(R.string.notif_unknown_name) ?: "")
                        val id = obj.optString("id")
                        if (id.isEmpty()) continue
                        val image = obj.optString("image")
                        val type = obj.optString("type", "")
                        val artists = obj.optJSONArray("artists")
                        val artistText = if (artists != null && artists.length() > 0) {
                            (0 until artists.length()).map { artists.getString(it) }.joinToString(", ")
                        } else ""
                        val sub = when {
                            type.isNotEmpty() && artistText.isNotEmpty() -> "$type • $artistText"
                            type.isNotEmpty() -> type
                            artistText.isNotEmpty() -> appContext?.getString(R.string.notif_by_artist, artistText) ?: artistText
                            else -> ""
                        }
                        val isBrowsable = obj.optBoolean("browsable", false)
                        val desc = MediaDescriptionCompat.Builder()
                            .setMediaId(id)
                            .setTitle(name)
                            .setSubtitle(sub)
                            .setIconUri(if (image.isNotEmpty()) Uri.parse(image) else null)
                            .build()
                        val flags = if (isBrowsable) MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                        else MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                        items.add(MediaBrowserCompat.MediaItem(desc, flags))
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Error parsing search results", e)
                }
            }
            val finalItems = items
            Handler(Looper.getMainLooper()).post { result.sendResult(finalItems) }
        }
    }

    private lateinit var mediaSession: MediaSessionCompat
    private var isPlaying = false
    private var isShuffle = false
    private var isSmartShuffle = false
    private var isShuffleAvailable = true
    private var isFavorite = false
    private var coverBitmap: Bitmap? = null
    private var currentTitle = ""
    private var currentArtist = ""
    private var currentAlbum = ""
    private var currentPosition: Long = 0L
    private var currentDuration: Long = 0L
    private var lastCoverUrl = ""
    private var lastActiveContextId: String? = null
    private var isRepeat = "false"
    private var wakeLock: PowerManager.WakeLock? = null
    private var serverWakeLock: PowerManager.WakeLock? = null
    @Suppress("DEPRECATION")
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null
    private var netCallback: android.net.ConnectivityManager.NetworkCallback? = null
    private var serverOn = false
    /** ready, connecting, reconnecting, offline, signin or starting: drives the idle server notification. */
    private var connLabel = "starting"
    private var lastServerNotif = ""
    private val playerRestarts = ArrayDeque<Long>()
    private val keepAliveTick = object : Runnable {
        override fun run() {
            try { keepAlive("tick") } catch (e: Exception) { Logger.e(TAG, "keepalive failed", e) }
            mainHandler.removeCallbacks(this)
            mainHandler.postDelayed(this, KEEPALIVE_MS)
        }
    }
    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastWidgetPushAt = 0L

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_PLAY_PAUSE -> {
                    webView?.evaluateJavascript("actPlayPause(${!isPlaying})", null)
                }
                ACTION_NEXT -> webView?.evaluateJavascript("actSkipForward()", null)
                ACTION_PREV -> webView?.evaluateJavascript("actSkipBack()", null)
                ACTION_SHUFFLE -> webView?.evaluateJavascript("actToggleShuffle()", null)
                ACTION_REPEAT -> webView?.evaluateJavascript("actRepeat()", null)
                ACTION_FAVORITE -> webView?.evaluateJavascript("actAddToFav()", null)
                ACTION_WIDGET_REFRESH -> pushWidgetState(force = true)
                ACTION_SERVER_OFF -> ServerMode.setOn(this@MediaNotificationService, false)
            }
        }
    }

    private val audioBecomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                val prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
                if (prefs.getBoolean("BtAutoPause", true)) autoPauseOnce("audio becoming noisy")
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (prefs.getBoolean("BtAutoPause", true)) autoPauseOnce("acl disconnected")
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (prefs.getBoolean("BtAutoResume", false)) autoResumeOnce("acl connected")
                }
            }
        }
    }

    private val remoteOutputTypes = intArrayOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_HEARING_AID,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY
    )

    private val knownRouteIds = mutableSetOf<Int>()
    private var lastAutoPauseAt = 0L
    private var lastAutoResumeAt = 0L

    private val audioRouteCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            val fresh = addedDevices.filter { knownRouteIds.add(it.id) }
            if (fresh.isEmpty()) return
            val prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
            if (!prefs.getBoolean("BtAutoResume", false)) return
            if (fresh.none { isRemoteOutput(it.type) }) return
            autoResumeOnce("route added: " + fresh.joinToString("/") { it.type.toString() })
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            knownRouteIds.removeAll(removedDevices.map { it.id }.toSet())
            if (!isPlaying) return
            val prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
            if (!prefs.getBoolean("BtAutoPause", true)) return
            if (removedDevices.none { isRemoteOutput(it.type) }) return
            val stillRemote = getSystemService(AudioManager::class.java)
                .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .any { isRemoteOutput(it.type) }
            if (stillRemote) return
            autoPauseOnce("route removed: " + removedDevices.joinToString("/") { it.type.toString() })
        }
    }

    private fun isRemoteOutput(type: Int): Boolean = remoteOutputTypes.contains(type)

    private fun btLog(msg: String) {
        Logger.s("bt", msg)
    }

    private fun autoPauseOnce(source: String) {
        val now = System.currentTimeMillis()
        if (now - lastAutoPauseAt < 1500L) {
            btLog("pause skipped, duplicate trigger ($source)")
            return
        }
        lastAutoPauseAt = now
        btLog("pause trigger: $source (webView=${if (webView != null) "bound" else "null"})")
        pausePlayback()
    }

    private fun autoResumeOnce(source: String) {
        val now = System.currentTimeMillis()
        if (now - lastAutoResumeAt < 1500L) {
            btLog("resume skipped, duplicate trigger ($source)")
            return
        }
        lastAutoResumeAt = now
        btLog("resume trigger: $source")
        resumePlayback()
    }

    private var lastMediaStatusJson: String? = null
    private var firstHeadsetCallback = true
    private var accentCache = 0

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == ServerMode.KEY || key == "LoggedIn") {
            mainHandler.post { applyServerMode() }
        }
        if (key == "PaletteSeed" || key == "MaterialYou") {
            accentCache = 0
            mainHandler.post {
                showNotification()
            }
        }
        if (key == "AndAuto") {
            val andAuto = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
                .getBoolean("AndAuto", true)
            if (andAuto) {
                lastMediaStatusJson?.let { updateFromMediaStatus(it) }
            }
        }
    }

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_HEADSET_PLUG) {
                val state = intent.getIntExtra("state", -1)
                if (firstHeadsetCallback) {
                    firstHeadsetCallback = false
                    return
                }
                if (state == 1) {
                    val prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
                    if (prefs.getBoolean("HpAutoResume", false)) autoResumeOnce("headset plugged")
                }
            }
        }
    }

    private fun accent(): Int {
        if (accentCache == 0) {
            accentCache = try {
                AccentTheme.resolveHex(this).toColorInt()
            } catch (_: Exception) {
                0xFFE0E0E0.toInt()
            }
        }
        return accentCache
    }

    private fun tintedIcon(resId: Int): IconCompat {
        val d = AppCompatResources.getDrawable(this, resId)!!.mutate()
        d.setTint(accent())
        val bmp = createBitmap(d.intrinsicWidth, d.intrinsicHeight)
        Canvas(bmp).also { canvas ->
            d.setBounds(0, 0, bmp.width, bmp.height)
            d.draw(canvas)
        }
        return IconCompat.createWithBitmap(bmp)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        appContext = applicationContext

        try {
            createNotificationChannel()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to create notification channel", e)
        }

        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotificationSafe(), getStartForegroundServiceType())
        } catch (e: Throwable) {
            Logger.e(TAG, "Failed to start foreground", e)
        }

        try {
            setupMediaSession()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to setup media session", e)
        }

        try {
            registerReceivers()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to register receivers", e)
        }
        try {
            registerDisconnectReceivers()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to register disconnect receivers", e)
        }
        getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)
        PlayerHost.headless = headlessHooks
        applyServerMode()
        mainHandler.postDelayed(keepAliveTick, 5_000L)
        Logger.i(TAG, "media service ready: session, notification and receivers up")
    }

    /** The page the service talks to: the screen's player, or the server's own when there's no screen. */
    private fun player(): WebView? = webView ?: PlayerHost.webView

    /** Turns Server Mode's locks, network watch and screenless player on or off to match the setting. */
    private fun applyServerMode() {
        val on = ServerMode.isOn(this)
        val changed = on != serverOn
        serverOn = on
        if (on) {
            refreshServerLocks()
            registerNetworkWatch()
            if (changed) Logger.i(TAG, "server mode on")
            ensurePlayer("server on")
        } else {
            releaseServerLocks()
            unregisterNetworkWatch()
            if (changed) Logger.i(TAG, "server mode off")
            if (PlayerHost.isHeadless) {
                // Nothing on screen and the server is off: stop everything.
                PlayerHost.destroy()
                webView = null
                stopSelf()
                return
            }
        }
        PlayerHost.applyPriority(this)
        lastServerNotif = ""
        mainHandler.post { showNotification() }
    }

    /** Builds a player without a screen when Server Mode is on and SpotiOS isn't open. */
    private fun ensurePlayer(reason: String) {
        if (!ServerMode.isOn(this)) return
        if (player() != null || PlayerHost.screens > 0) return
        if (!ServerMode.isLoggedIn(this)) {
            connLabel = "signin"
            return
        }
        val now = System.currentTimeMillis()
        while (playerRestarts.isNotEmpty() && now - playerRestarts.first() > 10 * 60_000L) playerRestarts.removeFirst()
        if (playerRestarts.size >= 5) {
            Logger.w(TAG, "player keeps failing, not restarting it again for now")
            connLabel = "reconnecting"
            return
        }
        playerRestarts.addLast(now)
        Logger.i(TAG, "starting the server player without a screen ($reason)")
        val bridge = PlayerHost.bridge?.also { it.attach(null) } ?: com.project.lol.bridge.SpotifyBridge(WeakReference(null))
        webView = PlayerHost.create(applicationContext, bridge, null, PlayerHost.PLAYER_URL)
        connLabel = "connecting"
    }

    private val headlessHooks = object : PlayerHost.Headless {
        override fun onHeadlessLoginRequired() {
            Logger.w(TAG, "server player needs sign-in")
            connLabel = "signin"
            PlayerHost.destroy()
            webView = null
            showNotification()
        }

        override fun onHeadlessRendererGone() {
            Logger.w(TAG, "server player renderer gone, restarting it")
            webView = null
            connLabel = "reconnecting"
            mainHandler.postDelayed({ ensurePlayer("renderer gone") }, 3_000L)
        }

        override fun onHeadlessError(code: Int, description: String) {
            Logger.w(TAG, "server player load error $code $description")
            connLabel = "offline"
            mainHandler.postDelayed({
                val wv = player() ?: return@postDelayed
                if (PlayerHost.isHeadless && connLabel == "offline") wv.reload()
            }, 15_000L)
        }
    }

    /**
     * Every 20 s: pings Spotify's connection from the page (window.spoKeepAlive), which also
     * reconnects it if it died, and in Server Mode keeps the CPU awake and the screenless
     * player alive. Spotify's own pings run on page timers that Android slows down in the
     * background; this tick runs regardless, so SpotiOS stays in other devices' lists.
     */
    private fun keepAlive(reason: String) {
        val server = ServerMode.isOn(this)
        if (server) refreshServerLocks()
        val wv = player()
        if (wv == null) {
            if (server) ensurePlayer("keepalive")
            if (server) updateServerNotification()
            return
        }
        if (server && PlayerHost.isHeadless && !ServerMode.isLoggedIn(this)) {
            headlessHooks.onHeadlessLoginRequired()
            return
        }
        val js = "(function(){try{return window.spoKeepAlive?window.spoKeepAlive($server,'$reason'):'';}catch(e){return '';}})()"
        wv.evaluateJavascript(js) { raw -> onKeepAliveReport(raw) }
    }

    private fun onKeepAliveReport(raw: String?) {
        val text = runCatching { org.json.JSONTokener(raw ?: "").nextValue() as? String }.getOrNull()
        if (text.isNullOrEmpty()) {
            // The late bundle isn't in yet (page loading) or the page is on the login screen.
            if (connLabel != "signin") connLabel = "connecting"
            updateServerNotification()
            return
        }
        connReport = text
        val o = runCatching { org.json.JSONObject(text) }.getOrNull() ?: return
        val state = o.optInt("state", -1)
        val msgAgo = o.optLong("msgAgo", -1)
        connLabel = when {
            !o.optBoolean("signedIn", true) -> "signin"
            !o.optBoolean("online", true) -> "offline"
            state == 1 && msgAgo in 0..75_000 -> "ready"
            state == 0 -> "connecting"
            o.optString("did") == "reload" -> "reconnecting"
            else -> "reconnecting"
        }
        if (o.optString("did").isNotEmpty()) Logger.i(TAG, "keepalive: ${o.optString("did")} ($text)")
        updateServerNotification()
    }

    private fun updateServerNotification() {
        if (!ServerMode.isOn(this) || currentTitle.isNotEmpty()) return
        if (lastServerNotif == connLabel) return
        lastServerNotif = connLabel
        showNotification()
    }

    private fun refreshServerLocks() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = serverWakeLock ?: pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "spotios:server").also {
                it.setReferenceCounted(false)
                serverWakeLock = it
            }
            // Re-armed every tick, so it lapses by itself if the ticks ever stop.
            wl.acquire(5 * 60_000L)
        } catch (e: Exception) {
            Logger.e(TAG, "server wake lock failed", e)
        }
        if (wifiLock == null && Build.VERSION.SDK_INT < 34) {
            try {
                val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                @Suppress("DEPRECATION")
                wifiLock = wm.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF, "spotios:server").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            } catch (e: Exception) {
                Logger.e(TAG, "wifi lock failed", e)
            }
        }
    }

    private fun releaseServerLocks() {
        try { serverWakeLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) {}
        serverWakeLock = null
        try { wifiLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) {}
        wifiLock = null
    }

    /** When the phone switches networks, check the Spotify connection right away instead of at the next tick. */
    private fun registerNetworkWatch() {
        if (netCallback != null) return
        val cm = getSystemService(android.net.ConnectivityManager::class.java) ?: return
        val cb = object : android.net.ConnectivityManager.NetworkCallback() {
            private var first = true
            override fun onAvailable(network: android.net.Network) {
                if (first) { first = false; return }
                Logger.i(TAG, "network changed, checking the Spotify connection")
                mainHandler.postDelayed({ keepAlive("net") }, 2_000L)
            }
            override fun onLost(network: android.net.Network) {
                Logger.i(TAG, "network lost")
            }
        }
        try {
            cm.registerDefaultNetworkCallback(cb)
            netCallback = cb
        } catch (e: Exception) {
            Logger.e(TAG, "network watch failed", e)
        }
    }

    private fun unregisterNetworkWatch() {
        val cb = netCallback ?: return
        netCallback = null
        runCatching { getSystemService(android.net.ConnectivityManager::class.java)?.unregisterNetworkCallback(cb) }
    }

    @Suppress("DEPRECATION")
    private fun getStartForegroundServiceType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d(TAG, "onStartCommand startId=$startId action=${intent?.action ?: "none"} taskRemoved=$taskRemoved")
        val server = ServerMode.isOn(this)
        if (taskRemoved && !server) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_SERVER_START -> mainHandler.post { ensurePlayer("start request") }
            ACTION_SPOTIFY_SEEN -> mainHandler.post {
                player()?.evaluateJavascript("window.spoSrvCheck&&window.spoSrvCheck('spotify')", null)
            }
            // Restarted by Android after it killed the app: bring the server back.
            null -> if (server) mainHandler.post { ensurePlayer("restarted by Android") }
        }
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotificationSafe(), getStartForegroundServiceType())
        } catch (e: Throwable) {
            Logger.e(TAG, "Failed to re-assert foreground", e)
        }
        try {
            MediaButtonReceiver.handleIntent(mediaSession, intent)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to handle media button intent", e)
        }
        // In Server Mode, ask Android to restart the service if it ever kills the app.
        return if (server) START_STICKY else START_NOT_STICKY
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        val andAuto = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
            .getBoolean("AndAuto", true)
        if (!andAuto) return null
        val extras = Bundle().apply {
            putBoolean("android.media.browse.SEARCH_SUPPORTED", true)
            putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
            putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 2)
            putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1)
        }
        return BrowserRoot(MEDIA_ID_ROOT, extras)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId == MEDIA_ID_ROOT) {
            val items = mutableListOf<MediaBrowserCompat.MediaItem>()
            items.add(createBrowsableItem(MEDIA_ID_PLAYLISTS, getString(com.project.lol.R.string.aa_playlists)))
            items.add(createBrowsableItem(MEDIA_ID_ALBUMS, getString(com.project.lol.R.string.aa_albums)))
            items.add(createBrowsableItem(MEDIA_ID_ARTISTS, getString(com.project.lol.R.string.aa_artists)))
            items.add(createBrowsableItem(MEDIA_ID_PODCASTS, getString(com.project.lol.R.string.aa_podcasts)))
            result.sendResult(items)
            return
        }
        if (parentId.startsWith("spotify:") || parentId == "your_library" ||
            parentId.contains("collection")
        ) {
            lastActiveContextId = parentId
        }
        result.detach()
        pendingCallbacks[parentId] = result
        wakeAndRun("if (typeof window.fetchMediaItems === 'function') window.fetchMediaItems('$parentId');")
        Handler(Looper.getMainLooper()).postDelayed({
            pendingCallbacks.remove(parentId)?.sendResult(mutableListOf())
        }, 20000L)
    }

    private fun createBrowsableItem(id: String, title: String): MediaBrowserCompat.MediaItem {
        val extras = Bundle().apply {
            putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 2)
            putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1)
        }
        val desc = MediaDescriptionCompat.Builder()
            .setMediaId(id)
            .setTitle(title)
            .setExtras(extras)
            .build()
        return MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    override fun onSearch(
        query: String,
        extras: Bundle?,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (query.isEmpty()) {
            result.sendResult(mutableListOf())
            return
        }
        result.detach()
        pendingSearchCallbacks[query] = result
        wakeAndRun("if (typeof window.searchMediaItems === 'function') window.searchMediaItems('$query');")
    }

    private fun wakeAndRun(js: String) {
        val wv = player() ?: return
        Handler(Looper.getMainLooper()).post {
            try {
                wv.resumeTimers()
                wv.onResume()
                wv.dispatchWindowVisibilityChanged(android.view.View.VISIBLE)
                wv.evaluateJavascript(js, null)
            } catch (e: Exception) {
                Logger.e(TAG, "Error waking WebView in wakeAndRun", e)
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            coverBitmap = null
            lastCoverUrl = ""
        }
    }

    override fun onDestroy() {
        releaseWakeLock()
        releaseServerLocks()
        unregisterNetworkWatch()
        mainHandler.removeCallbacks(keepAliveTick)
        if (PlayerHost.headless === headlessHooks) PlayerHost.headless = null
        if (PlayerHost.isHeadless) {
            PlayerHost.destroy()
            webView = null
        }
        instance = null
        try { unregisterReceiver(actionReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(bluetoothReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(audioBecomingNoisyReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(headsetReceiver) } catch (_: Exception) {}
        try { getSystemService(AudioManager::class.java).unregisterAudioDeviceCallback(audioRouteCallback) } catch (_: Exception) {}
        getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefsListener)
        if (::mediaSession.isInitialized) {
            try { mediaSession.isActive = false } catch (_: Exception) {}
            try { mediaSession.release() } catch (_: Exception) {}
        }
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
            getSystemService(NotificationManager::class.java)
                .cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
        val ctx = applicationContext
        widgetScope.launch { runCatching { WidgetUpdater.push(ctx, WidgetState()) } }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_media_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_media_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    @Suppress("DEPRECATION")
    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "SpotilolSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(MediaSessionCallback())
            isActive = true
        }
        sessionToken = mediaSession.sessionToken
    }

    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPrepare() {
            wakeAndRun("actPlayPause(true);")
        }

        override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
            onPlayFromMediaId(mediaId, extras)
        }

        override fun onPlay() {
            wakeAndRun("actPlayPause(true);")
        }

        override fun onPause() {
            wakeAndRun("actPlayPause(false);")
        }

        override fun onSkipToNext() {
            wakeAndRun("actSkipForward();")
        }

        override fun onSkipToPrevious() {
            wakeAndRun("actSkipBack();")
        }

        override fun onStop() {
            wakeAndRun("actPlayPause(false);")
        }

        override fun onSeekTo(pos: Long) {
            wakeAndRun("actSeek($pos);")
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            when (action) {
                CUSTOM_ACTION_TOGGLE_FAV, "ADDTOFAV_ACTION" -> wakeAndRun("actAddToFav();")
                CUSTOM_ACTION_TOGGLE_SHUFFLE, "SHUFFLE_ACTION" -> wakeAndRun("actToggleShuffle();")
                CUSTOM_ACTION_REPEAT, "REPEAT_ACTION" -> wakeAndRun("actRepeat();")
            }
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            val context = lastActiveContextId
            if (context != null && mediaId != null) {
                wakeAndRun("playFromUri('$mediaId', '$context');")
            } else {
                wakeAndRun("playFromUri('$mediaId');")
            }
        }
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREV)
            addAction(ACTION_SHUFFLE)
            addAction(ACTION_REPEAT)
            addAction(ACTION_FAVORITE)
            addAction(ACTION_WIDGET_REFRESH)
            addAction(ACTION_SERVER_OFF)
            addAction(Intent.ACTION_MEDIA_BUTTON)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(actionReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(actionReceiver, filter)
        }
    }

    private fun registerDisconnectReceivers() {
        val noisyFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(audioBecomingNoisyReceiver, noisyFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(audioBecomingNoisyReceiver, noisyFilter)
        }

        val btFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, btFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(bluetoothReceiver, btFilter)
        }

        val hsFilter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
        ContextCompat.registerReceiver(this, headsetReceiver, hsFilter, ContextCompat.RECEIVER_EXPORTED)

        try {
            val am = getSystemService(AudioManager::class.java)
            knownRouteIds.clear()
            knownRouteIds.addAll(am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.id })
            am.registerAudioDeviceCallback(audioRouteCallback, mainHandler)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to register audio route callback", e)
        }
    }

    private fun pausePlayback() {
        isPlaying = false
        updatePlaybackState()
        showNotification()
        if (::mediaSession.isInitialized) {
            try {
                mediaSession.controller.transportControls.pause()
            } catch (_: Exception) {}
        }
        webView?.evaluateJavascript("actPlayPause(false)", null)
    }

    private fun resumePlayback() {
        isPlaying = true
        updatePlaybackState()
        showNotification()
        if (::mediaSession.isInitialized) {
            try {
                mediaSession.controller.transportControls.play()
            } catch (_: Exception) {}
        }
        webView?.evaluateJavascript("actPlayPause(true)", null)
    }

    fun updateFromMediaStatus(json: String) {
        try {
            lastMediaStatusJson = json
            val obj = org.json.JSONObject(json)
            currentTitle = obj.optString("track", "")
            currentArtist = obj.optString("artist", "")
            currentAlbum = obj.optString("album", "")
            val coverUrl = obj.optString("cover", "")

            if (coverUrl.isNotEmpty() && coverUrl != "null" && coverUrl != lastCoverUrl) {
                lastCoverUrl = coverUrl
                loadCoverArt(coverUrl)
            } else if (coverUrl.isEmpty() || coverUrl == "null") {
                lastCoverUrl = ""
                coverBitmap = null
            }

            isPlaying = obj.optBoolean("playing", false)
            isFavorite = obj.optBoolean("fav", false)
            isRepeat = obj.optString("repeat", "false")
            val shuffleVal = obj.optString("shuffle", "off")
            isShuffle = shuffleVal == "shuffle" || shuffleVal == "smart"
            isSmartShuffle = shuffleVal == "smart"
            isShuffleAvailable = shuffleVal != "disabled"
            currentDuration = obj.optLong("duration", 0L)
            currentPosition = obj.optLong("position", 0L)

            if (isPlaying) acquireWakeLock() else releaseWakeLock()

            updatePlaybackState()
            updateMetadata()
            showNotification()
            pushWidgetState(force = true)
        } catch (_: Exception) {}
    }

    fun updatePlaybackPosition(position: Long) {
        currentPosition = position
        updatePlaybackState()
        pushWidgetState()
    }

    private fun pushWidgetState(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastWidgetPushAt < 2000L) return
        if (!isPlaying && WidgetSource.get(this) == WidgetState.SOURCE_OFFLINE) return
        lastWidgetPushAt = now
        val state = WidgetState(
            title = currentTitle,
            artist = currentArtist,
            playing = isPlaying,
            favorite = isFavorite,
            shuffle = when {
                !isShuffleAvailable -> "disabled"
                isSmartShuffle -> "smart"
                isShuffle -> "shuffle"
                else -> "off"
            },
            repeat = isRepeat,
            position = currentPosition,
            duration = currentDuration,
            accent = accent(),
            source = WidgetState.SOURCE_WEB,
            cover = coverBitmap
        )
        val ctx = applicationContext
        widgetScope.launch { runCatching { WidgetUpdater.push(ctx, state) } }
    }

    private fun updatePlaybackState() {
        val favIcon = if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
        val shuffleIcon = when {
            isSmartShuffle -> R.drawable.ic_shuffle_smart_active
            isShuffle -> R.drawable.ic_shuffle_active
            else -> R.drawable.ic_shuffle
        }
        val repeatIcon = when (isRepeat) {
            "true" -> R.drawable.ic_repeat
            "mixed" -> R.drawable.ic_repeat_one
            else -> R.drawable.ic_repeat_off
        }
        val state = PlaybackStateCompat.Builder()
            .setActions(PLAYBACK_ACTIONS)
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING
                else PlaybackStateCompat.STATE_PAUSED,
                currentPosition, if (isPlaying) 1f else 0f
            )
            .addCustomAction(
                CUSTOM_ACTION_TOGGLE_FAV,
                if (isFavorite) getString(R.string.notif_action_unlike) else getString(R.string.notif_action_like),
                favIcon
            )
            .addCustomAction(
                CUSTOM_ACTION_TOGGLE_SHUFFLE,
                when {
                    isSmartShuffle -> getString(R.string.notif_shuffle_disable_smart)
                    isShuffle -> getString(R.string.notif_shuffle_disable)
                    else -> getString(R.string.notif_shuffle_enable)
                },
                shuffleIcon
            )
            .addCustomAction(
                CUSTOM_ACTION_REPEAT,
                when (isRepeat) {
                    "true" -> getString(R.string.notif_repeat_disable)
                    "mixed" -> getString(R.string.notif_repeat_disable_one)
                    else -> getString(R.string.notif_repeat_enable)
                },
                repeatIcon
            )
            .build()
        if (::mediaSession.isInitialized) {
            try { mediaSession.setPlaybackState(state) } catch (_: Exception) {}
        }
    }

    private fun updateMetadata() {
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentAlbum)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentDuration)
        coverBitmap?.let { bmp ->
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bmp)
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bmp)
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bmp)
        }
        if (::mediaSession.isInitialized) {
            try { mediaSession.setMetadata(builder.build()) } catch (_: Exception) {}
        }
    }

    private fun loadCoverArt(url: String) {
        Thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.connect()
                val stream = conn.inputStream
                val raw = BitmapFactory.decodeStream(stream)
                stream.close()
                conn.disconnect()
                if (raw != null) {
                    val target = 512
                    val scale = min(target.toFloat() / raw.width, target.toFloat() / raw.height)
                    val w = (raw.width * scale).toInt()
                    val h = (raw.height * scale).toInt()
                    val scaled = Bitmap.createScaledBitmap(raw, w, h, true)
                    if (scaled != raw) raw.recycle()
                    coverBitmap = scaled
                    updateMetadata()
                    showNotification()
                    pushWidgetState(force = true)
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun showNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotificationSafe(): Notification {
        return try {
            buildNotification()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to build notification", e)
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .build()
        }
    }

    private fun buildMediaStyle(showShuffle: Boolean): MediaStyle {
        val compact = if (showShuffle) intArrayOf(0, 1, 2, 3) else intArrayOf(0, 1, 2)
        val style = MediaStyle()
            .setShowActionsInCompactView(*compact)
            .setShowCancelButton(true)
            .setCancelButtonIntent(getActionPendingIntent(ACTION_PLAY_PAUSE))
        if (::mediaSession.isInitialized) {
            style.setMediaSession(mediaSession.sessionToken)
        }
        return style
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val server = ServerMode.isOn(this)
        if (server && currentTitle.isEmpty()) return buildServerNotification(contentIntent)

        val prevAction = NotificationCompat.Action.Builder(
            tintedIcon(R.drawable.ic_skip_prev), getString(R.string.notif_action_previous), getActionPendingIntent(ACTION_PREV)
        ).build()

        val playPauseAction = NotificationCompat.Action.Builder(
            tintedIcon(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
            if (isPlaying) getString(R.string.notif_action_pause) else getString(R.string.notif_action_play),
            getActionPendingIntent(ACTION_PLAY_PAUSE)
        ).build()

        val nextAction = NotificationCompat.Action.Builder(
            tintedIcon(R.drawable.ic_skip_next), getString(R.string.notif_action_next), getActionPendingIntent(ACTION_NEXT)
        ).build()

        val shuffleAction = NotificationCompat.Action.Builder(
            tintedIcon(
                when {
                    isSmartShuffle -> R.drawable.ic_shuffle_smart_active
                    isShuffle -> R.drawable.ic_shuffle_active
                    else -> R.drawable.ic_shuffle
                }
            ),
            when {
                isSmartShuffle -> getString(R.string.notif_shuffle_disable_smart)
                isShuffle -> getString(R.string.notif_shuffle_disable)
                else -> getString(R.string.notif_shuffle_enable)
            },
            getActionPendingIntent(ACTION_SHUFFLE)
        ).build()

        val favAction = NotificationCompat.Action.Builder(
            tintedIcon(if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite),
            if (isFavorite) getString(R.string.notif_action_unlike) else getString(R.string.notif_action_like),
            getActionPendingIntent(ACTION_FAVORITE)
        ).build()

        val actions = mutableListOf<NotificationCompat.Action>()
        actions.add(prevAction)
        actions.add(playPauseAction)
        actions.add(nextAction)
        if (isShuffleAvailable) actions.add(shuffleAction)
        actions.add(favAction)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle.ifEmpty { getString(R.string.app_name) })
            .setContentText(currentArtist)
            .setSubText(if (server) getString(R.string.server_notif_subtext) else getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(accent())
            .setStyle(buildMediaStyle(isShuffleAvailable))
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
        actions.forEach { builder.addAction(it) }

        coverBitmap?.let { builder.setLargeIcon(it) }

        return builder.build()
    }

    /** Server Mode with nothing playing: says whether SpotiOS is ready on Spotify Connect. */
    private fun buildServerNotification(contentIntent: PendingIntent): Notification {
        val name = getSharedPreferences("spotilol_prefs", MODE_PRIVATE).getString("SpoDeviceName", null)
            ?.trim()?.take(40)?.takeIf { it.isNotEmpty() } ?: "SpotiOS"
        val text = when (connLabel) {
            "ready" -> getString(R.string.server_notif_ready, name)
            "signin" -> getString(R.string.server_notif_signin)
            "offline" -> getString(R.string.server_notif_offline)
            "reconnecting" -> getString(R.string.server_notif_reconnecting)
            else -> getString(R.string.server_notif_connecting)
        }
        val off = NotificationCompat.Action.Builder(
            tintedIcon(R.drawable.ic_pause), getString(R.string.server_notif_turn_off), getActionPendingIntent(ACTION_SERVER_OFF)
        ).build()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.server_title))
            .setContentText(text)
            .setSubText(if (connLabel == "ready") getString(R.string.server_notif_live) else null)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(accent())
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(off)
            .build()
    }

    private fun getActionPendingIntent(action: String): PendingIntent {
        val mediaButtonAction = when (action) {
            ACTION_PLAY_PAUSE -> PlaybackStateCompat.ACTION_PLAY_PAUSE
            ACTION_NEXT -> PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            ACTION_PREV -> PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            else -> null
        }
        if (mediaButtonAction != null) {
            return MediaButtonReceiver.buildMediaButtonPendingIntent(this, mediaButtonAction)
        }
        val intent = Intent(action).setPackage(packageName)
        return PendingIntent.getBroadcast(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld != true) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "spotilol:media_playback"
            ).apply { acquire(60 * 60 * 1000L) }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
            wakeLock = null
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val stopOnSwipe = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
            .getBoolean("SwipeStop", true)
        if (ServerMode.isOn(this)) {
            // Server Mode keeps running when SpotiOS is swiped away; the player was handed
            // over by MainActivity.onDestroy (PlayerHost.release).
            Logger.i(TAG, "task removed, server keeps running")
            lastServerNotif = ""
            mainHandler.post { showNotification() }
        } else if (stopOnSwipe) {
            taskRemoved = true
            if (::mediaSession.isInitialized) {
                try { mediaSession.isActive = false } catch (_: Exception) {}
            }
            releaseWakeLock()
            try {
                getSystemService(NotificationManager::class.java)
                    .cancel(NOTIFICATION_ID)
            } catch (_: Exception) {}
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }
}
