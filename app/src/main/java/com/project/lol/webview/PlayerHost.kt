package com.project.lol.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.MutableContextWrapper
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.project.lol.bridge.SpotifyBridge
import com.project.lol.proxy.LocalProxyManager
import com.project.lol.util.Logger
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

/**
 * Owns the one Spotify web player WebView in the process.
 *
 * Normally MainActivity shows it and destroys it when it closes. In Server Mode the player
 * has to outlive the screen: when SpotiOS is swiped away the WebView is detached and kept
 * running for the media service, and when SpotiOS opens again the new screen adopts the same
 * WebView, so playback and the Spotify Connect session never drop. If Android killed the
 * app, the service builds a new player without a screen ([create] with no owner).
 *
 * The WebView is built on a [MutableContextWrapper], so it can move between the screen and
 * the application context without holding a dead Activity.
 */
object PlayerHost {

    private const val TAG = "player"
    const val LOGIN_URL = "https://accounts.spotify.com/login"
    const val PLAYER_URL = "https://open.spotify.com/"

    /** What the screen showing the player gets told. Null while the player runs without one. */
    interface Owner {
        fun onPlayerProgress(progress: Int) {}
        fun onPlayerShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) { callback?.onCustomViewHidden() }
        fun onPlayerHideCustomView() {}
        fun onPlayerLoginRequired(view: WebView) { view.loadUrl(LOGIN_URL) }
        fun onPlayerRendererGone() {}
        fun onPlayerError(code: Int, description: String) {}
        fun onPlayerUserScriptLink(url: String) {}
    }

    /** Hooks for the media service while no screen owns the player. */
    interface Headless {
        fun onHeadlessLoginRequired()
        fun onHeadlessRendererGone()
        fun onHeadlessError(code: Int, description: String)
    }

    @Volatile var webView: WebView? = null
        private set
    var bridge: SpotifyBridge? = null
        private set
    private var client: SpotifyWebViewClient? = null
    private var owner: WeakReference<Owner>? = null
    var headless: Headless? = null
    /** When the current player was created; the Server screen shows it as uptime. */
    var createdAt = 0L
        private set

    val isHeadless: Boolean get() = webView != null && owner?.get() == null

    /** How many MainActivity screens exist; the media service only builds a player when there are none. */
    @Volatile var screens = 0

    /** The bridge for a screen: the running player's bridge, re-pointed at [activity], or a new one. */
    fun bridgeFor(activity: Activity): SpotifyBridge {
        val b = bridge?.takeIf { webView != null } ?: SpotifyBridge(WeakReference(activity))
        b.attach(activity)
        bridge = b
        return b
    }

    /**
     * Hands the running player to [activity] if there is one: re-parents it under the new
     * screen's context and points its callbacks at [owner]. Returns null when there is no
     * player to adopt, and the screen builds one with [create].
     */
    fun adopt(activity: Activity, owner: Owner): WebView? {
        val wv = webView ?: return null
        (wv.context as? MutableContextWrapper)?.baseContext = activity
        (wv.parent as? ViewGroup)?.removeView(wv)
        bridge?.attach(activity)
        this.owner = WeakReference(owner)
        try {
            wv.onResume()
            wv.resumeTimers()
        } catch (_: Exception) {}
        Logger.i(TAG, "screen adopted the running player (up ${(System.currentTimeMillis() - createdAt) / 1000}s)")
        return wv
    }

    /**
     * Builds the player WebView and starts loading [target]. [base] is the Activity when a
     * screen builds it, or the application context when the media service builds one
     * without a screen (then [owner] is null).
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun create(base: Context, bridge: SpotifyBridge, owner: Owner?, target: String): WebView {
        webView?.let { old -> if (old.parent == null && this.owner?.get() == null) destroy() }
        val wv = WebView(MutableContextWrapper(base))
        wv.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        wv.settings.apply {
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            allowFileAccess = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            setGeolocationEnabled(false)
            @Suppress("DEPRECATION")
            saveFormData = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        wv.setInitialScale(100)
        wv.setBackgroundColor(0xFF000000.toInt())
        if (WebViewFeature.isFeatureSupported(WebViewFeature.BACK_FORWARD_CACHE)) {
            WebSettingsCompat.setBackForwardCacheEnabled(wv.settings, true)
        }

        wv.addJavascriptInterface(bridge, "AndBridge")
        wv.webChromeClient = SpotifyWebChromeClient(
            onProgressChanged = { p -> currentOwner()?.onPlayerProgress(p) },
            onShowCustomView = { v, cb ->
                val o = currentOwner()
                if (o != null) o.onPlayerShowCustomView(v, cb) else cb?.onCustomViewHidden()
            },
            onHideCustomView = { currentOwner()?.onPlayerHideCustomView() }
        )
        val spotifyClient = SpotifyWebViewClient(
            onLoginRequired = {
                val o = currentOwner()
                if (o != null) o.onPlayerLoginRequired(wv) else headless?.onHeadlessLoginRequired()
            },
            onRenderProcessGone = {
                Handler(Looper.getMainLooper()).post {
                    val o = currentOwner()
                    forget(wv)
                    if (o != null) o.onPlayerRendererGone() else headless?.onHeadlessRendererGone()
                }
            },
            onWebViewError = { code, desc ->
                val o = currentOwner()
                if (o != null) o.onPlayerError(code, desc) else headless?.onHeadlessError(code, desc)
            },
            onUserScriptLink = { url -> currentOwner()?.onPlayerUserScriptLink(url) }
        )
        wv.webViewClient = spotifyClient
        // Before the first loadUrl, so it applies to the first page.
        spotifyClient.installDocumentStartScripts(wv)

        val executor = Executors.newSingleThreadExecutor()
        val prefs = base.getSharedPreferences("spotilol_prefs", Context.MODE_PRIVATE)
        val useProxy = prefs.getString("ConnectionMode", "normal") == "proxy"
        if (useProxy && LocalProxyManager.isRunning) {
            val proxyConfig = ProxyConfig.Builder().addProxyRule("localhost:${LocalProxyManager.port}").build()
            ProxyController.getInstance().setProxyOverride(proxyConfig, executor) { }
        } else {
            ProxyController.getInstance().clearProxyOverride(executor) { }
        }

        webView = wv
        this.bridge = bridge
        client = spotifyClient
        this.owner = owner?.let { WeakReference(it) }
        createdAt = System.currentTimeMillis()
        applyPriority(base)
        if (owner == null) {
            // No screen: give the page a phone-sized viewport so Spotify lays out as usual.
            val dm = base.resources.displayMetrics
            val w = dm.widthPixels.coerceAtLeast(720)
            val h = dm.heightPixels.coerceAtLeast(1280)
            wv.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            wv.layout(0, 0, w, h)
        }
        Logger.i(TAG, "player created (${if (owner == null) "no screen, server mode" else "screen"}) target=$target")
        wv.loadUrl(target)
        return wv
    }

    /**
     * The screen is closing. In Server Mode with the media service running, keeps the player
     * alive without a screen and returns true; otherwise returns false and the caller destroys it.
     */
    fun release(activity: Activity, keep: Boolean): Boolean {
        val wv = webView ?: return false
        if (!keep) return false
        (wv.parent as? ViewGroup)?.removeView(wv)
        (wv.context as? MutableContextWrapper)?.baseContext = activity.applicationContext
        bridge?.attach(null)
        owner = null
        applyPriority(activity.applicationContext)
        Handler(Looper.getMainLooper()).postDelayed({ keepVisible() }, 300L)
        Logger.i(TAG, "screen closed, player keeps running for server mode")
        return true
    }

    /**
     * Server Mode: tells the player it is on screen even while it isn't (screen off, SpotiOS
     * in the background or closed), so Chromium doesn't throttle the page that holds the
     * Spotify Connect session. Android marks the view hidden again whenever its window hides,
     * so the media service re-applies this on every keepalive tick.
     */
    fun keepVisible() {
        val wv = webView ?: return
        try {
            wv.onResume()
            wv.resumeTimers()
            wv.dispatchWindowVisibilityChanged(View.VISIBLE)
        } catch (_: Exception) {}
    }

    /** Drops our references to [wv] after the caller destroyed it (or its renderer died). */
    fun forget(wv: WebView?) {
        if (wv != null && wv !== webView) return
        webView = null
        client = null
        owner = null
        createdAt = 0L
    }

    /** Stops and destroys the player when no screen is showing it (server mode turned off). */
    fun destroy() {
        val wv = webView ?: return
        Logger.i(TAG, "destroying the player")
        forget(wv)
        try {
            wv.stopLoading()
            wv.removeJavascriptInterface("AndBridge")
            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_VIEW_RENDERER_TERMINATE)) {
                runCatching { WebViewCompat.getWebViewRenderProcess(wv)?.terminate() }
            }
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.removeAllViews()
            wv.destroy()
        } catch (e: Exception) {
            Logger.e(TAG, "destroy failed", e)
        }
    }

    /**
     * Keeps the renderer at foreground priority even while nothing is on screen, so Android
     * doesn't kill the page that holds the Spotify Connect session (WebView's default, made
     * explicit because the player now lives on after the screen closes).
     */
    @Suppress("UNUSED_PARAMETER")
    fun applyPriority(context: Context) {
        val wv = webView ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching { wv.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false) }
    }

    private fun currentOwner(): Owner? = owner?.get()
}
