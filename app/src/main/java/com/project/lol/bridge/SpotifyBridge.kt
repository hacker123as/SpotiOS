package com.project.lol.bridge

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.project.lol.R
import com.project.lol.service.MediaNotificationService
import com.project.lol.service.ServerMode
import com.project.lol.webview.helpers.AdIdStore
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import com.project.lol.offline.DownloadManager
import com.project.lol.util.Logger

class SpotifyBridge(activityRef: WeakReference<Activity>) {

    companion object {
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"

        private val FILTERED_HEADERS = setOf(
            "x-requested-with",
            "sec-ch-ua-full-version-list",
            "sec-ch-ua-platform-version",
            "sec-ch-ua-arch",
            "sec-ch-ua-bitness",
            "sec-ch-ua-model"
        )

        private const val TAG = "bridge"
        private const val CALL = "bridge.call"

        /** Threads for nFetchAsync: Spotify Connect requests run here, several at once, never on the page's JS thread. */
        private val NET_POOL = Executors.newFixedThreadPool(4) { r ->
            Thread(r, "spo-nfetch").apply { isDaemon = true }
        }
        private val MAIN = Handler(Looper.getMainLooper())
    }

    @Volatile private var activityRef = activityRef

    /** Points the bridge at the screen now showing the player, or null while it runs without one. */
    fun attach(activity: Activity?) {
        activityRef = WeakReference(activity)
    }

    private fun appContext(): android.content.Context? =
        activityRef.get()?.applicationContext ?: com.project.lol.SpotilolApp.context

    /** Runs JS in the Spotify WebView; set by MainActivity. */
    var onJs: ((String) -> Unit)? = null
    var onLoginDetected: (() -> Unit)? = null
    var onPlayLoaded: (() -> Unit)? = null
    var onMediaStatus: ((String) -> Unit)? = null
    var onMediaPosition: ((Long) -> Unit)? = null
    var onTimerDialogRequest: (() -> Unit)? = null
    var onEnterPipRequest: (() -> Unit)? = null
    var onEnterPipVideoRequest: ((Int, Int) -> Unit)? = null
    var onDownloadTrack: ((String) -> Unit)? = null
    var onDownloadCollection: ((String) -> Unit)? = null
    var onOpenSettingsRequest: (() -> Unit)? = null
    var onOpenDevScripts: (() -> Unit)? = null
    var onSleepTimerFinished: (() -> Unit)? = null

    @JavascriptInterface
    fun loginDetected() {
        val ctx = appContext() ?: return
        Logger.i(TAG, "login detected")
        ctx.getSharedPreferences("spotilol_prefs", Activity.MODE_PRIVATE)
            .edit()
            .putBoolean("LoggedIn", true)
            .apply()
        activityRef.get()?.runOnUiThread {
            onLoginDetected?.invoke()
        }
    }

    /** Server Mode settings and phone facts for the Server screen (see ServerMode.statusJson). */
    @JavascriptInterface
    fun serverStatus(): String {
        val ctx = appContext() ?: return "{}"
        return ServerMode.statusJson(ctx)
    }

    /** Read at document start, before the Server screen loads (ConnectKeepAlive keeps the page visible). */
    @JavascriptInterface
    fun isServerMode(): Boolean {
        val ctx = appContext() ?: return false
        return ServerMode.isOn(ctx)
    }

    @JavascriptInterface
    fun setServerMode(on: Boolean) {
        val ctx = appContext() ?: return
        ServerMode.setOn(ctx, on)
    }

    /** Stop server on the Server screen: SpotiOS leaves Spotify Connect and closes. */
    @JavascriptInterface
    fun stopServer() {
        val ctx = appContext() ?: return
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            ServerMode.stop(ctx, "server screen")
            activityRef.get()?.let { runCatching { it.finishAndRemoveTask() } }
        }
    }

    /** Sound controls on the Server screen: settings plus what this phone supports (see SoundFx.stateJson). */
    @JavascriptInterface
    fun soundFx(): String {
        val ctx = appContext() ?: return "{}"
        return com.project.lol.service.SoundFx.stateJson(ctx)
    }

    /** Saves sound settings from the Server screen (any of on/boost/bass/treble/surround) and applies them. */
    @JavascriptInterface
    fun setSoundFx(json: String) {
        val ctx = appContext() ?: return
        com.project.lol.service.SoundFx.set(ctx, json)
    }

    @JavascriptInterface
    fun setStartWithSpotify(on: Boolean) {
        val ctx = appContext() ?: return
        ServerMode.setStartWithSpotify(ctx, on)
        // Ask for notification access the first time it's needed.
        if (on && !ServerMode.hasNotificationAccess(ctx)) openNotificationAccess()
    }

    /** Server screen > Start on boot (status JSON key "onBoot"). */
    @JavascriptInterface
    fun setStartOnBoot(on: Boolean) {
        val ctx = appContext() ?: return
        ServerMode.setStartOnBoot(ctx, on)
    }

    @JavascriptInterface
    fun openNotificationAccess() {
        val activity = activityRef.get() ?: return
        activity.runOnUiThread {
            runCatching { activity.startActivity(ServerMode.notificationAccessIntent(activity)) }
                .onFailure { runCatching { activity.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS)) } }
        }
    }

    @JavascriptInterface
    fun openBatterySettings() {
        val activity = activityRef.get() ?: return
        activity.runOnUiThread {
            runCatching { activity.startActivity(com.project.lol.ui.onboarding.batteryOptimizationIntent(activity)) }
                .onFailure { runCatching { activity.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS)) } }
        }
    }

    /** Back on the Server screen: leave SpotiOS running in the background, like Home. */
    @JavascriptInterface
    fun moveToBack() {
        val activity = activityRef.get() ?: return
        activity.runOnUiThread { activity.moveTaskToBack(true) }
    }

    /** Opens the Spotify app (Server screen > Open Spotify), or its store page. */
    @JavascriptInterface
    fun openSpotifyApp() {
        val activity = activityRef.get() ?: return
        activity.runOnUiThread {
            val launch = activity.packageManager.getLaunchIntentForPackage(ServerMode.SPOTIFY_PACKAGE)
            if (launch != null) {
                runCatching { activity.startActivity(launch) }
            } else {
                runCatching {
                    activity.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + ServerMode.SPOTIFY_PACKAGE)))
                }
            }
        }
    }

    @JavascriptInterface
    fun deferMessage(msg: String?) {
        val activity = activityRef.get() ?: return
        if (msg == "adblock") return
        val display = when (msg) {
            "unlock" -> activity.getString(R.string.bridge_player_unlocked)
            "reload" -> activity.getString(R.string.bridge_reloading)
            else -> msg
        }
        Logger.d(CALL, "deferMessage: $display")
        activity.runOnUiThread {
            Toast.makeText(activity, display, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun isWoke(): Boolean {
        val activity = activityRef.get() ?: return false
        val visible = activity.window?.decorView?.visibility == View.VISIBLE
        Logger.v(CALL, "isWoke -> $visible")
        return visible
    }

    @JavascriptInterface
    fun wakeUp() {
        Logger.v(CALL, "wakeUp")
    }

    @JavascriptInterface
    fun wakeOff() {
        Logger.v(CALL, "wakeOff")
    }

    @JavascriptInterface
    fun cssInjected() {
        Logger.v(CALL, "cssInjected")
    }

    @JavascriptInterface
    fun dbg(level: String?, msg: String?) {
        if (!Logger.isEnabled()) return
        Logger.js(level, msg)
    }

    @JavascriptInterface
    fun clearDebugLog() {
        Logger.clear()
        Logger.d(CALL, "logger buffer cleared from js")
    }

    @JavascriptInterface
    fun recAdContentIds(json: String?) {
        val payload = json ?: return
        val arr = try { JSONArray(payload) } catch (e: Exception) { return }
        val ids = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) {
            val v = arr.optString(i, "")
            if (v.isNotEmpty()) ids.add(v)
        }
        if (ids.isNotEmpty()) {
            AdIdStore.addAll(ids)
            Logger.d(CALL, "recAdContentIds: ${ids.size}")
        }
    }

    @JavascriptInterface
    fun playLoaded() {
        val activity = activityRef.get() ?: return
        Logger.i(TAG, "play loaded, web player ready")
        activity.runOnUiThread {
            onPlayLoaded?.invoke()
        }
    }

    @JavascriptInterface
    fun recMediaPosition(position: Long) {
        Logger.v(CALL, "position=$position")
        onMediaPosition?.invoke(position)
        MediaNotificationService.instance?.updatePlaybackPosition(position)
    }

    @JavascriptInterface
    fun recMediaStatus(json: String?) {
        json?.let {
            Logger.d(CALL, "media status (${it.length} chars): ${it.take(180)}")
            onMediaStatus?.invoke(it)
            MediaNotificationService.instance?.updateFromMediaStatus(it)
        }
    }

    @JavascriptInterface
    fun onMediaItemsLoaded(parentId: String?, json: String?) {
        Logger.d(CALL, "media items parent=$parentId size=${json?.length ?: 0}")
        parentId?.let { MediaNotificationService.onMediaItemsLoaded(it, json ?: "[]") }
    }

    @JavascriptInterface
    fun onSearchCompleted(query: String?, json: String?) {
        Logger.d(CALL, "search completed query=$query size=${json?.length ?: 0}")
        query?.let { MediaNotificationService.onSearchCompleted(it, json ?: "[]") }
    }

    @JavascriptInterface
    fun manageTShut(enabled: Boolean) {
        Logger.v(CALL, "manageTShut=$enabled")
    }

    @JavascriptInterface
    fun manageTSleep(enabled: Boolean) {
        Logger.v(CALL, "manageTSleep=$enabled")
    }

    @JavascriptInterface
    fun recAccountName(name: String) {
        val activity = activityRef.get() ?: return
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            Logger.i(TAG, "account name: $trimmed")
            activity.getSharedPreferences("spotilol_prefs", Activity.MODE_PRIVATE)
                .edit()
                .putString("CurrentAccountName", trimmed)
                .apply()
        }
    }

    @JavascriptInterface
    fun openTimerDialog() {
        val activity = activityRef.get() ?: return
        Logger.d(CALL, "openTimerDialog")
        activity.runOnUiThread {
            onTimerDialogRequest?.invoke()
        }
    }

    @JavascriptInterface
    fun shareText(subject: String?, text: String?) {
        val activity = activityRef.get() ?: return
        val body = text?.trim().orEmpty()
        if (body.isEmpty()) return
        Logger.d(CALL, "shareText")
        activity.runOnUiThread {
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, body)
                if (!subject.isNullOrBlank()) putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
            }
            try {
                activity.startActivity(android.content.Intent.createChooser(send, null))
            } catch (e: Exception) {
                Logger.e(TAG, "share failed", e)
            }
        }
    }

    @JavascriptInterface
    fun openSettings() {
        val activity = activityRef.get() ?: return
        activity.runOnUiThread { onOpenSettingsRequest?.invoke() }
    }

    /** Where the music plays right now, for the Server screen: `{"name":"Phone speaker","kind":"speaker"}`. */
    @JavascriptInterface
    fun audioOutput(): String {
        val ctx = appContext() ?: return "{}"
        return runCatching { com.project.lol.util.AudioOutput.currentJson(ctx) }.getOrDefault("{}")
    }

    /** System audio output picker (phone speaker, Bluetooth, wired, cast). */
    @JavascriptInterface
    fun openAudioOutput() {
        val activity = activityRef.get() ?: return
        activity.runOnUiThread {
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                val shown = runCatching {
                    android.media.MediaRouter2.getInstance(activity).showSystemOutputSwitcher()
                }.getOrDefault(false)
                if (shown) return@runOnUiThread
            }
            val panel = android.content.Intent("com.android.settings.panel.action.MEDIA_OUTPUT")
                .putExtra("com.android.settings.panel.extra.PACKAGE_NAME", activity.packageName)
            if (runCatching { activity.startActivity(panel) }.isSuccess) return@runOnUiThread
            runCatching {
                activity.sendBroadcast(
                    android.content.Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG")
                        .setPackage("com.android.systemui")
                        .putExtra("package_name", activity.packageName)
                )
            }.onFailure {
                runCatching { activity.startActivity(android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }
            }
        }
    }

    /** Is a VPN, Private DNS or ad blocker keeping the server from Spotify? JSON, see NetCheck.snapshot. Never waits. */
    @JavascriptInterface
    fun netCheck(force: Boolean): String {
        val ctx = appContext() ?: return "{}"
        return com.project.lol.util.NetCheck.snapshot(ctx, force)
    }

    @JavascriptInterface
    fun openVpnSettings() {
        val activity = activityRef.get() ?: return
        activity.runOnUiThread {
            startFirst(
                activity,
                android.content.Intent(android.provider.Settings.ACTION_VPN_SETTINGS),
                android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS),
                android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
            )
        }
    }

    /** Android has no public Private DNS screen; "Network & internet" holds the setting. */
    @JavascriptInterface
    fun openPrivateDnsSettings() {
        val activity = activityRef.get() ?: return
        activity.runOnUiThread {
            // Only a screen of the system's own Settings, not some other app claiming the action.
            val privateDns = android.content.Intent("android.settings.PRIVATE_DNS_SETTINGS").takeIf {
                val app = runCatching { activity.packageManager.resolveActivity(it, 0) }.getOrNull()?.activityInfo?.applicationInfo
                app != null && (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            }
            startFirst(
                activity,
                *listOfNotNull(
                    privateDns,
                    android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS),
                    android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                ).toTypedArray()
            )
        }
    }

    /** Opens the blocker app named in netCheck's "blockers" (e.g. "AdGuard"), else VPN settings. */
    @JavascriptInterface
    fun openBlockerApp(name: String) {
        val activity = activityRef.get() ?: return
        activity.runOnUiThread {
            val intent = com.project.lol.util.NetCheck.blockerIntent(activity, name)
            if (intent == null || runCatching { activity.startActivity(intent) }.isFailure) openVpnSettings()
        }
    }

    /** Starts the first of [intents] that opens; true when one did. */
    private fun startFirst(activity: Activity, vararg intents: android.content.Intent): Boolean =
        intents.any { runCatching { activity.startActivity(it) }.isSuccess }

    @JavascriptInterface
    fun openDevScripts() {
        val activity = activityRef.get() ?: return
        activity.runOnUiThread { onOpenDevScripts?.invoke() }
    }

    @JavascriptInterface
    fun haptic() {
        val activity = activityRef.get() ?: return
        if (!activity.getSharedPreferences("spotilol_prefs", 0).getBoolean("SpoHaptics", true)) return
        activity.runOnUiThread {
            activity.window?.decorView?.performHapticFeedback(
                android.view.HapticFeedbackConstants.CONTEXT_CLICK
            )
        }
    }

    @JavascriptInterface
    fun sleepTimerFinished() {
        val activity = activityRef.get() ?: return
        Logger.i(CALL, "sleepTimerFinished (end of song)")
        activity.runOnUiThread { onSleepTimerFinished?.invoke() }
    }

    @JavascriptInterface
    fun enterPip() {
        val activity = activityRef.get() ?: return
        Logger.i(CALL, "enterPip")
        activity.runOnUiThread {
            onEnterPipRequest?.invoke()
        }
    }

    @JavascriptInterface
    fun enterPipVideo(w: Int, h: Int) {
        val activity = activityRef.get() ?: return
        Logger.i(CALL, "enterPipVideo ${w}x$h")
        activity.runOnUiThread {
            onEnterPipVideoRequest?.invoke(w, h)
        }
    }

    @JavascriptInterface
    fun downloadTrack(json: String?) {
        Logger.i(CALL, "downloadTrack (${json?.length ?: 0} chars)")
        json?.let { onDownloadTrack?.invoke(it) }
    }

    @Suppress("unused")
    @JavascriptInterface
    fun downloadCollection(json: String?) {
        Logger.i(CALL, "downloadCollection (${json?.length ?: 0} chars)")
        json?.let { onDownloadCollection?.invoke(it) }
    }

    @Suppress("unused")
    @JavascriptInterface
    fun skipDownload() {
        Logger.i(CALL, "skipDownload")
        DownloadManager.skipCurrent()
    }

    @Suppress("unused")
    @JavascriptInterface
    fun lyricsLookup(reqId: String, artist: String, title: String, durationSec: Double) {
        Logger.i(CALL, "lyricsLookup ${title.take(60)}")
        Thread {
            val result = com.project.lol.lyrics.LrcLib.lookup(artist, title, durationSec)
            val js = "window.spoLyricsResult&&window.spoLyricsResult(${JSONObject.quote(reqId)},$result)"
            activityRef.get()?.runOnUiThread { onJs?.invoke(js) }
        }.apply { isDaemon = true }.start()
    }

    @Suppress("unused")
    @JavascriptInterface
    fun cancelDownload() {
        Logger.i(CALL, "cancelDownload")
        DownloadManager.cancelAll()
    }

    /**
     * Like [nFetch], but returns at once and hands the response to the page later through
     * window.__nfCb(id, raw). nFetch blocks the page's JS thread for the whole round trip, and
     * starting a song sends several Spotify Connect requests, so each one used to hold up the
     * rest of the player (and the next request) while it waited. Returns false when there is no
     * player to answer to, and the page then falls back to nFetch.
     */
    @JavascriptInterface
    fun nFetchAsync(id: String, url: String, optsJson: String?): Boolean {
        val wv = com.project.lol.webview.PlayerHost.webView ?: return false
        return try {
            NET_POOL.execute {
                val raw = nFetch(url, optsJson)
                val js = "window.__nfCb&&window.__nfCb(" + JSONObject.quote(id) + "," + JSONObject.quote(raw) + ")"
                // Not wv.post: a View only runs posted work while it is on screen, and in Server
                // Mode the player usually isn't.
                MAIN.post {
                    if (com.project.lol.webview.PlayerHost.webView === wv) runCatching { wv.evaluateJavascript(js, null) }
                }
            }
            true
        } catch (e: Exception) {
            Logger.w(TAG, "nFetchAsync not started: ${e.javaClass.simpleName}")
            false
        }
    }

    @Suppress("unused")
    @JavascriptInterface
    fun nFetch(url: String, optsJson: String?): String {
        val errorResult = { e: Exception ->
            try {
                JSONObject().apply {
                    put("status", 0)
                    put("body", e.toString())
                    put("headers", JSONObject())
                }.toString()
            } catch (_: Exception) {
                "{\"status\":0,\"body\":\"error\",\"headers\":{}}"
            }
        }

        var conn: HttpURLConnection? = null
        return try {
            val opts = if (optsJson.isNullOrBlank()) JSONObject() else JSONObject(optsJson)
            val method = opts.optString("method", "GET")
            val body = if (opts.has("body") && !opts.isNull("body")) opts.getString("body") else null
            val headersJson =
                if (opts.has("headers") && !opts.isNull("headers")) opts.getJSONObject("headers") else JSONObject()

            Logger.d(TAG, "nFetch $method ${url.take(140)} body=${body?.length ?: 0} headers=${headersJson.length()}")

            conn = URL(url).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = method
                connectTimeout = 10000
                readTimeout = 10000
                val keys = headersJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (!FILTERED_HEADERS.contains(key.lowercase(Locale.ROOT))) {
                        setRequestProperty(key, headersJson.getString(key))
                    }
                }
                setRequestProperty("User-Agent", DESKTOP_UA)
                setRequestProperty("sec-ch-ua-platform", "\"Windows\"")
                setRequestProperty("sec-ch-ua-mobile", "?0")
                setRequestProperty("sec-ch-ua", "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Google Chrome\";v=\"150\"")
                if (url.contains("spclient.spotify.com") || url.contains("scdn.co") || url.contains("spotify.com")) {
                    setRequestProperty("Origin", "https://open.spotify.com")
                    setRequestProperty("Referer", "https://open.spotify.com/")
                }
                val cookie = CookieManager.getInstance().getCookie(url)
                if (!cookie.isNullOrEmpty()) setRequestProperty("Cookie", cookie)
                if (!body.isNullOrEmpty()) {
                    doOutput = true
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }

            val code = conn.responseCode
            val headerFields = conn.headerFields
            headerFields.forEach { (key, values) ->
                if (key != null && key.equals("Set-Cookie", ignoreCase = true)) {
                    values.forEach { CookieManager.getInstance().setCookie(url, it) }
                }
            }
            CookieManager.getInstance().flush()

            val stream = if (code >= 400) conn.errorStream else conn.inputStream
            val responseBody = stream?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
            Logger.d(TAG, "nFetch <- $code ${responseBody.length} bytes ${url.take(100)}")

            val responseHeaders = JSONObject()
            headerFields.forEach { (key, values) ->
                if (key != null && values.isNotEmpty()) responseHeaders.put(key, values.first())
            }
            JSONObject().apply {
                put("status", code)
                put("body", responseBody)
                put("headers", responseHeaders)
            }.toString()
        } catch (e: Exception) {
            Logger.e(TAG, "nFetch failed ${url.take(140)}", e)
            errorResult(e)
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }
}
