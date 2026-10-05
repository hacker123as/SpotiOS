package com.project.lol.ui

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebSettingsCompat
import com.project.lol.R
import com.project.lol.bridge.SpotifyBridge
import com.project.lol.offline.DownloadManager
import com.project.lol.profile.ProfileManager
import com.project.lol.proxy.LocalProxyManager
import com.project.lol.service.MediaNotificationService
import com.project.lol.service.ServerMode
import com.project.lol.webview.PlayerHost
import com.project.lol.ui.components.ChangelogDialog
import com.project.lol.ui.components.SettingsDialog
import com.project.lol.ui.components.UpdatePrompt
import com.project.lol.ui.theme.SpotifyTheme
import com.project.lol.update.UpdateManager
import com.project.lol.util.BuildInfo
import com.project.lol.util.ChangelogPrefs
import com.project.lol.util.Logger
import com.project.lol.util.UpdateChecker
import com.project.lol.webview.SpotifyWebViewClient
import com.project.lol.webview.helpers.DevLogPrelude
import com.project.lol.webview.helpers.LyricsTheme
import com.project.lol.webview.helpers.buildAmoledJs
import com.project.lol.webview.helpers.buildCustomCssJs
import com.project.lol.webview.injections.LogoutCheck
import compose.icons.TablerIcons
import compose.icons.tablericons.Menu2
import compose.icons.tablericons.Settings
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min
import org.json.JSONObject
import androidx.core.content.edit
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import com.project.lol.ui.components.ErrorScreen
import com.project.lol.ui.components.mapWebViewError
import com.project.lol.webview.helpers.AccentTheme
import com.project.lol.webview.helpers.LiquidGlassTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity(), PlayerHost.Owner {

    companion object {
        private const val TAG = "main"
    }

    private var webView: WebView? = null
    private var serviceStarted = false
    @Volatile private var pipCoverBitmap: Bitmap? = null
    @Volatile private var pipPlaying = false
    private var lastPipCoverUrl = ""
    private var pipOverlay: FrameLayout? = null
    private var pipCoverImg: ImageView? = null
    private var pipUsed = false
    private var pipVideoView: View? = null
    private var pipVideoCallback: android.webkit.WebChromeClient.CustomViewCallback? = null
    private var pipVideoAspect: Rational? = null
    private var pipVideoRequested = false
    private val pipVideoActive = mutableStateOf(false)

    private val serviceEnabledState = mutableStateOf(true)
    private val materialYouState = mutableStateOf(false)
    private val amoledState = mutableStateOf(false)
    private val hideTopBarState = mutableStateOf(false)
    private val landscapeModeState = mutableStateOf(false)
    private val keepScreenOnState = mutableStateOf(false)
    private val paletteSeedState = mutableStateOf<String?>(null)

    private val showSleepTimerDialog = mutableStateOf(false)
    private val showDevScripts = mutableStateOf(false)
    private val sleepTimerInputText = mutableStateOf("")
    private var sleepTimer: CountDownTimer? = null
    private val sleepTimerRemainingMs = mutableLongStateOf(0L)
    private val sleepTimerActive = mutableStateOf(false)
    private val sleepEndOfSong = mutableStateOf(false)
    private val spoTabBarState = mutableStateOf(true)
    private val spoPrefListener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
        if (key == "SpoTabBar") spoTabBarState.value = p.getBoolean("SpoTabBar", true)
    }

    private val loadingProgress = mutableIntStateOf(100)
    private val blockServiceWorkerState = mutableStateOf(true)
    private val webViewError = mutableStateOf<Pair<Int, String>?>(null)
    private var pendingLink: String? = null
    /** A user script waiting for the installer: a .user.js link, a shared Greasy Fork page or a .js file. */
    private val pendingScript = mutableStateOf<String?>(null)

    private lateinit var prefs: SharedPreferences

    private var changelogOnUpdate = false
    /** Set when SpotiOS restarts itself (account switch, clear data...): never hand the old player on. */
    private var restarting = false
    private var countedScreen = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        super.onCreate(savedInstanceState)
        PlayerHost.screens++
        countedScreen = true


        prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
        changelogOnUpdate = ChangelogPrefs.shouldShowOnUpdate(this)
        val useProxy = prefs.getString("ConnectionMode", "normal") == "proxy"

        // After an OOM kill, Android can resume directly at MainActivity
        if (useProxy && !LocalProxyManager.isRunning) {
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
            return
        }

        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        UpdateChecker(this).autoCheck { update -> UpdateManager.offer(update) }

        val loggedIn = prefs.getBoolean("LoggedIn", false)

        serviceEnabledState.value = prefs.getBoolean("ServiceOn", true)
        materialYouState.value = prefs.getBoolean("MaterialYou", false)
        amoledState.value = prefs.getBoolean("AmoledTheme", false)
        hideTopBarState.value = prefs.getBoolean("HideTopBar", true)
        landscapeModeState.value = prefs.getBoolean("LandscapeMode", false)
        keepScreenOnState.value = prefs.getBoolean("KeepScreenOn", false)
        paletteSeedState.value = prefs.getString("PaletteSeed", null)
        blockServiceWorkerState.value = prefs.getBoolean("BlockServiceWorker", true)
        spoTabBarState.value = prefs.getBoolean("SpoTabBar", true)
        prefs.registerOnSharedPreferenceChangeListener(spoPrefListener)
        applyOrientation()
        applyKeepScreenOn()

        pendingScript.value = com.project.lol.util.IncomingLinks.userScriptSource(intent)
        pendingLink = if (pendingScript.value == null) extractSpotifyLink(intent) else null
        if (pendingScript.value == null && pendingLink == null && intent?.action == Intent.ACTION_SEND) {
            Toast.makeText(this, "SpotiOS opens Spotify links and user scripts", Toast.LENGTH_SHORT).show()
        }
        if (pendingLink != null && !serviceEnabledState.value) {
            setServiceEnabled(true)
        }

        Logger.s(
            TAG,
            "session: loggedIn=$loggedIn service=${serviceEnabledState.value} mode=${prefs.getString("ConnectionMode", "normal")} " +
                "engine=${prefs.getString("PlayerMode", "spotilol")} proxyRunning=${LocalProxyManager.isRunning} " +
                "deeplink=${pendingLink ?: "none"} logging=${com.project.lol.util.Logger.isEnabled()}"
        )

        setContent {
            val serviceEnabled = serviceEnabledState.value
            val materialYou = materialYouState.value
            val amoled = amoledState.value
            val hideTopBar = hideTopBarState.value
            val landscapeMode = landscapeModeState.value
            val keepScreenOn = keepScreenOnState.value
            val paletteSeed = paletteSeedState.value
            val showDialog = showSleepTimerDialog.value
            val timerActive = sleepTimerActive.value
            val loadProgress = loadingProgress.intValue
            val blockServiceWorker = blockServiceWorkerState.value
            val pipFilling = pipVideoActive.value

            var settingsDialogOpen by remember { mutableStateOf(false) }
            var showMiniMenu by remember { mutableStateOf(false) }
            var showChangelog by rememberSaveable { mutableStateOf(changelogOnUpdate) }
            val versionName = remember {
                runCatching { packageManager.getPackageInfo(packageName, 0).versionName }
                    .getOrNull() ?: ""
            }
            val seedColor = paletteSeed?.let { hex ->
                runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
            }
            val accentColor = remember(materialYou, paletteSeed) {
                AccentTheme.resolveColor(this@MainActivity)
            }

            BackHandler(enabled = true) {
                val wv = webView
                when {
                    settingsDialogOpen -> settingsDialogOpen = false
                    wv == null -> moveTaskToBack(true)
                    else -> wv.evaluateJavascript(
                        "(function(){try{return !!(window.spoBack&&window.spoBack());}catch(e){return false;}})()"
                    ) { handled ->
                        if (handled != "true") {
                            if (wv.canGoBack()) wv.goBack() else moveTaskToBack(true)
                        }
                    }
                }
            }

            SpotifyTheme(useDynamicColor = materialYou, amoled = amoled, seedColor = seedColor) {
                pendingScript.value?.let { src ->
                    com.project.lol.ui.components.ScriptInstallDialog(src) { pendingScript.value = null }
                }
                if (showChangelog) {
                    ChangelogDialog(onDismiss = {
                        showChangelog = false
                        ChangelogPrefs.markShown(this@MainActivity)
                    })
                }
                UpdatePrompt()
                SettingsDialog(
                    visible = settingsDialogOpen,
                    onClose = { settingsDialogOpen = false },
                    prefs = prefs,
                    materialYou = materialYou,
                    onMaterialYouChange = { enabled ->
                        materialYouState.value = enabled
                        prefs.edit().putBoolean("MaterialYou", enabled).apply()
                    },
                    amoledThemeState = amoled,
                    onAmoledThemeChange = { enabled ->
                        amoledState.value = enabled
                        prefs.edit().putBoolean("AmoledTheme", enabled).apply()
                    },
                    hideTopBar = hideTopBar,
                    onHideTopBarChange = { enabled ->
                        hideTopBarState.value = enabled
                        prefs.edit().putBoolean("HideTopBar", enabled).apply()
                    },
                    landscapeMode = landscapeMode,
                    onLandscapeModeChange = { enabled ->
                        landscapeModeState.value = enabled
                        prefs.edit().putBoolean("LandscapeMode", enabled).apply()
                        applyOrientation()
                    },
                    keepScreenOn = keepScreenOn,
                    onKeepScreenOnChange = { enabled ->
                        keepScreenOnState.value = enabled
                        prefs.edit().putBoolean("KeepScreenOn", enabled).apply()
                        applyKeepScreenOn()
                    },
                    paletteSeed = paletteSeed,
                    onPaletteSeedChange = { hex ->
                        paletteSeedState.value = hex
                        if (hex.isNullOrBlank()) {
                            prefs.edit().remove("PaletteSeed").apply()
                        } else {
                            prefs.edit().putString("PaletteSeed", hex).apply()
                        }
                    },
                    onConnectionModeChange = { switchConnectionMode(it) },
                    onOfflineModeChange = { switchOfflineMode(it) },
                    onSaveProfile = { name, cookies -> saveProfile(name, cookies) },
                    onLoadProfile = { cookies -> loadProfile(cookies) },
                    onDeleteProfile = { name -> deleteProfile(name) },
                    onClearCache = { clearWebViewCache() },
                    onClearData = { clearAllData() },
                    onDebugToggle = { enabled ->
                        webView?.evaluateJavascript(
                            if (enabled) DevLogPrelude.js()
                            else "window.dbg=null;window.dbgv=null;window.dbgi=null;window.dbgw=null;window.dbge=null;window.DevLog=null;",
                            null
                        )
                    },
                    blockServiceWorker = blockServiceWorker,
                    onBlockServiceWorkerChange = { enabled ->
                        blockServiceWorkerState.value = enabled
                        prefs.edit { putBoolean("BlockServiceWorker", enabled) }
                    },
                ) {
                    Scaffold(
                        topBar = {
                            if (!hideTopBar && !pipFilling) {
                                CenterAlignedTopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(R.string.app_name),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.main_version_label, versionName, BuildInfo.id),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        settingsDialogOpen = true
                                    }) {
                                        Icon(
                                            imageVector = TablerIcons.Menu2,
                                            contentDescription = stringResource(R.string.main_settings_content_description),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                actions = {
                                Switch(
                                    checked = serviceEnabled,
                                    onCheckedChange = { newValue -> setServiceEnabled(newValue) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                            },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            }
                    }
) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (serviceEnabled) {
                            // In Server Mode the player (and its bridge) may still be running from
                            // before SpotiOS was swiped away: carry on with the same one.
                            val bridge = remember { PlayerHost.bridgeFor(this@MainActivity) }
                            bridge.onJs = { js -> webView?.evaluateJavascript(js, null) }

                            bridge.onTimerDialogRequest = {
                                showSleepTimerDialog.value = true
                                if (!timerActive) {
                                    sleepTimerInputText.value = ""
                                }
                            }

                            bridge.onOpenSettingsRequest = {
                                settingsDialogOpen = true
                            }

                            bridge.onOpenDevScripts = { showDevScripts.value = true }

                            bridge.onSleepTimerFinished = {
                                cancelSleepTimer()
                            }

                            bridge.onEnterPipRequest = {
                                enterPipMode()
                            }

                            bridge.onEnterPipVideoRequest = { w, h ->
                                enterPipVideoMode(w, h)
                            }

                            bridge.onMediaStatus = { json ->
                                handleMediaStatus(json)
                            }

                            bridge.onDownloadTrack = { payload ->
                                wireDownloadCallbacks()
                                startDownloadService()
                                DownloadManager.downloadCurrentTrack(this@MainActivity, payload)
                            }

                            bridge.onDownloadCollection = { payload ->
                                wireDownloadCallbacks()
                                startDownloadService()
                                DownloadManager.downloadCollection(this@MainActivity, payload)
                            }

                            AndroidView(
                                factory = { context ->
                                    val kept = PlayerHost.adopt(this@MainActivity, this@MainActivity)
                                    if (kept != null) {
                                        webView = kept
                                        MediaNotificationService.webView = kept
                                        pendingLink?.let { link ->
                                            pendingLink = null
                                            kept.post { navigateSpotifyLink(link) }
                                        }
                                        kept
                                    } else {
                                        val target = pendingLink
                                            ?: if (loggedIn) PlayerHost.PLAYER_URL
                                            else PlayerHost.LOGIN_URL
                                        pendingLink = null
                                        Logger.i(
                                            TAG,
                                            "webview ready: js=on dom=on multiWindow=on bfcache=" +
                                                WebViewFeature.isFeatureSupported(WebViewFeature.BACK_FORWARD_CACHE) +
                                                " proxy=$useProxy target=$target"
                                        )
                                        PlayerHost.create(context, bridge, this@MainActivity, target).also { webView = it }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                            )

                            LaunchedEffect(webView) {
                                webView?.let { startMediaService() }
                            }

                            LaunchedEffect(webView) {
                                val link = pendingLink
                                val wv = webView
                                if (link != null && wv != null) {
                                    pendingLink = null
                                    wv.loadUrl(link)
                                }
                            }

                            val progressAlpha by animateFloatAsState(
                                targetValue = if (loadProgress < 100) 1f else 0f,
                                animationSpec = tween(durationMillis = 600, delayMillis = 200),
                                label = "progressAlpha"
                            )
                            if (progressAlpha > 0.001f) {
                                LinearProgressIndicator(
                                    progress = { loadProgress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .align(Alignment.TopCenter)
                                        .alpha(progressAlpha),
                                    color = accentColor,   // was Color(0xFF22DD66)
                                    trackColor = Color.Transparent,
                                )
                            }

                            webViewError.value?.let { (code, desc) ->
                                com.project.lol.ui.components.ErrorScreen(
                                    errorType = com.project.lol.ui.components.mapWebViewError(code),
                                    errorCode = code,
                                    errorDescription = desc,
                                    onRetry = {
                                        webViewError.value = null
                                        webView?.reload()
                                    }
                                )
                            }

                            if (showDevScripts.value) {
                                com.project.lol.ui.components.DevScriptsDialog(onClose = { changed ->
                                    showDevScripts.value = false
                                    if (changed) webView?.reload()
                                })
                            }

                            if (showDialog) {
                                SleepTimerDialog(
                                    timerActive = timerActive,
                                    timerRemainingMs = sleepTimerRemainingMs.longValue,
                                    inputText = sleepTimerInputText.value,
                                    onInputChange = { sleepTimerInputText.value = it },
                                    endOfSong = sleepEndOfSong.value,
                                    onSetTimer = { minutes ->
                                        showSleepTimerDialog.value = false
                                        if (minutes > 0) {
                                            startSleepTimer(minutes)
                                        } else {
                                            cancelSleepTimer()
                                        }
                                    },
                                    onEndOfSong = {
                                        showSleepTimerDialog.value = false
                                        startEndOfSongTimer()
                                    },
                                    onCancelTimer = {
                                        showSleepTimerDialog.value = false
                                        cancelSleepTimer()
                                    },
                                    onDismiss = {
                                        showSleepTimerDialog.value = false
                                    }
                                )
                            }
                        } else {
                            AndroidView(
                                factory = { context ->
                                    LayoutInflater.from(context)
                                        .inflate(R.layout.service_disabled, null).apply {
                                            val tvVersion = findViewById<TextView>(R.id.tvWebViewVersion)
                                            val pkg = WebViewCompat.getCurrentWebViewPackage(context)
                                            tvVersion.text = context.getString(
                                                R.string.main_webview_version,
                                                pkg?.versionName ?: context.getString(R.string.main_webview_version_unknown)
                                            )
                                        }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (hideTopBar && !(spoTabBarState.value && serviceEnabled)) {
                            QuickAccessOverlay(
                                showMenu = showMiniMenu,
                                onToggleMenu = { showMiniMenu = !showMiniMenu },
                                onOpenSettings = {
                                    showMiniMenu = false
                                    settingsDialogOpen = true
                                },
                                serviceEnabled = serviceEnabled,
                                onServiceToggle = { newValue -> setServiceEnabled(newValue) },
                                onDismissMenu = { showMiniMenu = false }
                            )
                        }
                    }
                }
            }
            }

        }
    }

    // PlayerHost.Owner: the player's page events while this screen shows it.
    override fun onPlayerProgress(progress: Int) {
        loadingProgress.intValue = progress
    }

    override fun onPlayerShowCustomView(view: View?, callback: android.webkit.WebChromeClient.CustomViewCallback?) {
        handleCustomViewShown(view, callback)
    }

    override fun onPlayerHideCustomView() {
        handleCustomViewHidden()
    }

    override fun onPlayerLoginRequired(view: WebView) {
        view.loadUrl(PlayerHost.LOGIN_URL)
    }

    override fun onPlayerRendererGone() {
        webViewError.value = null
        destroyWebView()
    }

    override fun onPlayerError(code: Int, description: String) {
        webViewError.value = code to description
    }

    override fun onPlayerUserScriptLink(url: String) {
        runOnUiThread { pendingScript.value = url }
    }

    /** open.spotify.com links, spotify: URIs and Spotify links shared as text. */
    private fun extractSpotifyLink(intent: Intent?): String? =
        com.project.lol.util.IncomingLinks.spotifyUrl(intent)

    private fun setServiceEnabled(newValue: Boolean) {
        Logger.i(TAG, "service toggle: $newValue")
        serviceEnabledState.value = newValue
        prefs.edit().putBoolean("ServiceOn", newValue).apply()
        if (!newValue) {
            stopService(Intent(this, MediaNotificationService::class.java))
            serviceStarted = false
            destroyWebView()
        } else {
        }
    }

    private fun switchConnectionMode(mode: String) {
        Logger.i(TAG, "connection mode -> $mode, restarting app")
        restarting = true
        prefs.edit().putString("ConnectionMode", mode).apply()
        prefs.edit().putBoolean("ServiceOn", false).apply()
        stopService(Intent(this, MediaNotificationService::class.java))
        LocalProxyManager.stop()
        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun switchOfflineMode(enabled: Boolean) {
        Logger.i(TAG, "offline mode -> $enabled, restarting app")
        restarting = true
        prefs.edit().putBoolean("OfflineMode", enabled).apply()
        stopService(Intent(this, MediaNotificationService::class.java))
        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun saveProfile(name: String, cookies: String) {
        Logger.i(TAG, "saving account profile: $name")
        ProfileManager.saveProfile(this, name, cookies)
        Toast.makeText(this, getString(R.string.main_account_saved), Toast.LENGTH_SHORT).show()
    }

    private fun loadProfile(cookies: String) {
        Logger.i(TAG, "loading account profile (${cookies.length} chars)")
        if (!ProfileManager.applyProfile(this, cookies)) {
            Logger.w(TAG, "profile could not be applied")
            Toast.makeText(this, getString(R.string.main_profile_load_failed), Toast.LENGTH_SHORT).show()
            return
        }
        restarting = true
        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun deleteProfile(name: String) {
        Logger.i(TAG, "deleting account profile: $name")
        ProfileManager.deleteProfile(this, name)
        Toast.makeText(this, getString(R.string.main_profile_deleted), Toast.LENGTH_SHORT).show()
    }

    private fun clearWebViewCache() {
        Logger.i(TAG, "clearing webview cache and history")
        val wv = WebView(applicationContext)
        wv.clearCache(true)
        wv.clearHistory()
        wv.destroy()
        Toast.makeText(this, getString(R.string.main_cache_cleared), Toast.LENGTH_SHORT).show()
    }

    private fun clearAllData() {
        Logger.w(TAG, "clearing all data (cache, storage, cookies, login state)")
        restarting = true
        val wv = WebView(applicationContext)
        wv.clearCache(true)
        wv.clearHistory()
        wv.clearFormData()
        wv.destroy()
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        prefs.edit().putBoolean("LoggedIn", false).apply()
        Toast.makeText(this, getString(R.string.main_all_data_cleared), Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        val totalMs = minutes * 60 * 1000L
        Logger.i(TAG, "sleep timer started: ${minutes}min")
        sleepTimerActive.value = true
        sleepTimerRemainingMs.longValue = totalMs


        webView?.evaluateJavascript("""
            if(window.timerBtn) timerBtn.style.color='var(--spl-accent,#2d6)';
            var t=document.getElementById('spl-timer');
            if(t) t.classList.add('spl-active');
        """.trimIndent(), null)

        sleepTimer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                sleepTimerRemainingMs.longValue = millisUntilFinished
            }

            override fun onFinish() {
                Logger.i(TAG, "sleep timer finished, pausing playback")
                sleepTimerActive.value = false
                sleepTimerRemainingMs.longValue = 0L
                webView?.evaluateJavascript("""
                    if(window.timerBtn) timerBtn.style.color='';
                    var t=document.getElementById('spl-timer');
                    if(t) t.classList.remove('spl-active');
                """.trimIndent(), null)
                webView?.evaluateJavascript("actPlayPause(false)", null)
            }
        }.start()
    }

    private fun startEndOfSongTimer() {
        cancelSleepTimer()
        Logger.i(TAG, "sleep timer: end of song")
        sleepTimerActive.value = true
        sleepEndOfSong.value = true
        webView?.evaluateJavascript("""
            if(window.spoSleepEndOfSong) spoSleepEndOfSong(true);
            var t=document.getElementById('spl-timer');
            if(t) t.classList.add('spl-active');
        """.trimIndent(), null)
    }

    private fun cancelSleepTimer() {
        if (sleepTimer != null) Logger.i(TAG, "sleep timer cancelled")
        sleepTimer?.cancel()
        sleepTimer = null
        sleepTimerActive.value = false
        sleepEndOfSong.value = false
        sleepTimerRemainingMs.longValue = 0L
        webView?.evaluateJavascript("""
            if(window.spoSleepEndOfSong) spoSleepEndOfSong(false);
            if(window.timerBtn) timerBtn.style.color='';
            var t=document.getElementById('spl-timer');
            if(t) t.classList.remove('spl-active');
        """.trimIndent(), null)
    }

    @Composable
    private fun SleepTimerDialog(
        timerActive: Boolean,
        timerRemainingMs: Long,
        inputText: String,
        endOfSong: Boolean,
        onInputChange: (String) -> Unit,
        onSetTimer: (Int) -> Unit,
        onEndOfSong: () -> Unit,
        onCancelTimer: () -> Unit,
        onDismiss: () -> Unit
    ) {
        val minutes = inputText.toIntOrNull() ?: 0
        val presets = listOf(5, 15, 30, 45, 60)
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    stringResource(R.string.main_sleep_timer_title),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (timerActive) {
                        Text(
                            text = stringResource(R.string.main_sleep_timer_emoji),
                            style = MaterialTheme.typography.displaySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        if (endOfSong) {
                            Text(
                                text = stringResource(R.string.main_timer_end_of_song_active),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            val remainingSecs = timerRemainingMs / 1000
                            Text(
                                text = stringResource(R.string.main_timer_active),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.main_timer_remaining, remainingSecs / 60, remainingSecs % 60),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.main_timer_presets_hint),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(14.dp))
                        val rows = (presets.map { it.toString() } + "eos").chunked(3)
                        rows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { item ->
                                    FilledTonalButton(
                                        onClick = {
                                            if (item == "eos") onEndOfSong() else onSetTimer(item.toInt())
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = when (item) {
                                                "eos" -> stringResource(R.string.main_timer_end_of_song)
                                                "60" -> "1 h"
                                                else -> "$item min"
                                            },
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { new ->
                                if (new.length <= 4 && new.all { it.isDigit() }) onInputChange(new)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            label = { Text(stringResource(R.string.main_timer_custom)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = { Text(stringResource(R.string.main_minutes_suffix), style = MaterialTheme.typography.bodyMedium) }
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (timerActive) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.main_close)) }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = onCancelTimer,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text(stringResource(R.string.main_cancel_timer)) }
                    } else {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.main_cancel)) }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = { onSetTimer(minutes) },
                            enabled = minutes > 0,
                            shape = RoundedCornerShape(16.dp)
                        ) { Text(stringResource(R.string.main_set_timer)) }
                    }
                }
            },
            dismissButton = {}
        )
    }

    @Composable
    private fun QuickAccessOverlay(
        showMenu: Boolean,
        onToggleMenu: () -> Unit,
        onOpenSettings: () -> Unit,
        serviceEnabled: Boolean,
        onServiceToggle: (Boolean) -> Unit,
        onDismissMenu: () -> Unit
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(44.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleMenu),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_spotios_logo),
                    contentDescription = stringResource(R.string.main_quick_settings),
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (showMenu) {
                Popup(
                    alignment = Alignment.TopCenter,
                    offset = IntOffset(0, with(LocalDensity.current) { 64.dp.toPx() }.toInt()),
                    onDismissRequest = onDismissMenu
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(modifier = Modifier.width(220.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()                                                .clickable(onClick = onOpenSettings)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = TablerIcons.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.main_settings),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = TablerIcons.Menu2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onServiceToggle(!serviceEnabled) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.main_service),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = serviceEnabled,
                                    onCheckedChange = onServiceToggle,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun enterPipMode(video: Boolean = false) {
        Logger.i(TAG, "entering pip mode (video=$video)")
        pipVideoRequested = video
        pipVideoActive.value = video
        pipUsed = true
        if (lastPipCoverUrl.isNotEmpty() && pipCoverBitmap == null) {
            fetchPipCover(lastPipCoverUrl)
        }
        if (!video) showPipOverlay()
        val ok = enterPictureInPictureMode(buildPipParams())
        if (!ok) {
            hidePipOverlay()
            pipVideoActive.value = false
            if (video) setPipFillVideo(false)
        }
    }

    private fun enterPipVideoMode(w: Int, h: Int) {
        pipVideoAspect = if (w > 0 && h > 0) Rational(w, h) else Rational(9, 16)
        pipVideoRequested = true
        val wv = webView
        if (wv == null) {
            enterPipMode(video = true)
            return
        }
        wv.post {
            wv.evaluateJavascript(
                "window.__splPipFillVideo&&window.__splPipFillVideo(true)"
            ) { enterPipMode(video = true) }
        }
    }

    private fun handleCustomViewShown(
        view: View?,
        callback: android.webkit.WebChromeClient.CustomViewCallback?
    ) {
        Logger.i(TAG, "fullscreen video view shown")
        pipVideoView = view
        pipVideoCallback = callback
        showPipVideoOverlay()
        if (isInPictureInPictureMode) updatePipParams()
    }

    private fun handleCustomViewHidden() {
        Logger.i(TAG, "fullscreen video view hidden")
        pipVideoView = null
        pipVideoCallback = null
        hidePipOverlay()
        if (isInPictureInPictureMode && pipVideoRequested) {
            setPipFillVideo(true)
            updatePipParams()
            return
        }
        pipVideoRequested = false
        pipVideoActive.value = false
        if (isInPictureInPictureMode) fallbackPipToCover()
    }

    private fun setPipFillVideo(on: Boolean) {
        webView?.evaluateJavascript(
            "window.__splPipFillVideo&&window.__splPipFillVideo($on)",
            null
        )
    }

    private fun fallbackPipToCover() {
        if (!isInPictureInPictureMode) return
        pipVideoAspect = Rational(1, 1)
        if (pipCoverBitmap == null && lastPipCoverUrl.isNotEmpty()) {
            fetchPipCover(lastPipCoverUrl)
        }
        showPipOverlay()
        updatePipParams()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            if (pipVideoView == null && !pipVideoRequested) showPipOverlay()
            updatePipParams()
        } else {
            hidePipOverlay()
            pipVideoView = null
            pipVideoAspect = null
            pipVideoRequested = false
            pipVideoActive.value = false
            setPipFillVideo(false)
            pipVideoCallback?.onCustomViewHidden()
            pipVideoCallback = null
        }
    }

    private fun buildPipParams(): PictureInPictureParams =
        PictureInPictureParams.Builder()
            .setAspectRatio(pipVideoAspect ?: Rational(1, 1))
            .setActions(buildPipActions())
            .build()

    private fun buildPipActions(): List<RemoteAction> {
        val prev = RemoteAction(
            Icon.createWithResource(this, R.drawable.ic_skip_prev),
            getString(R.string.main_pip_previous), getString(R.string.main_pip_previous),
            pipActionIntent(MediaNotificationService.ACTION_PREV)
        )
        val playPause = RemoteAction(
            Icon.createWithResource(
                this,
                if (pipPlaying) R.drawable.ic_pause else R.drawable.ic_play
            ),
            if (pipPlaying) getString(R.string.main_pip_pause) else getString(R.string.main_pip_play),
            if (pipPlaying) getString(R.string.main_pip_pause) else getString(R.string.main_pip_play),
            pipActionIntent(MediaNotificationService.ACTION_PLAY_PAUSE)
        )
        val next = RemoteAction(
            Icon.createWithResource(this, R.drawable.ic_skip_next),
            getString(R.string.main_pip_next), getString(R.string.main_pip_next),
            pipActionIntent(MediaNotificationService.ACTION_NEXT)
        )
        return listOf(prev, playPause, next)
    }

    private fun pipActionIntent(action: String): PendingIntent {
        val intent = Intent(action).setPackage(packageName)
        return PendingIntent.getBroadcast(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            setPictureInPictureParams(buildPipParams())
        }
    }

    private fun showPipVideoOverlay() {
        if (pipOverlay != null) return
        val content = findViewById<ViewGroup>(android.R.id.content) ?: return
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
        }
        pipVideoView?.let {
            overlay.addView(
                it,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        pipOverlay = overlay
        content.addView(
            overlay,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun showPipOverlay() {
        if (pipOverlay != null) return
        val content = findViewById<ViewGroup>(android.R.id.content) ?: return
        val img = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(pipCoverBitmap)
        }
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
            addView(
                img,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        pipCoverImg = img
        pipOverlay = overlay
        content.addView(
            overlay,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun hidePipOverlay() {
        pipOverlay?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }
        pipOverlay = null
        pipCoverImg = null
    }

    private fun handleMediaStatus(json: String) {
        try {
            val obj = JSONObject(json)
            pipPlaying = obj.optBoolean("playing", false)
            Logger.v(TAG, "media status: playing=$pipPlaying title=${obj.optString("title", "").take(48)}")
            val coverUrl = obj.optString("cover", "")
            if (coverUrl.isNotEmpty() && coverUrl != "null" && coverUrl != lastPipCoverUrl) {
                lastPipCoverUrl = coverUrl
                if (pipUsed || isInPictureInPictureMode) {
                    fetchPipCover(coverUrl)
                } else {
                    pipCoverBitmap = null
                }
            }
            runOnUiThread { updatePipParams() }
        } catch (e: Exception) {
            Logger.d(TAG, "media status not parseable (${json.length} chars): ${e.message}")
        }
    }

    private fun fetchPipCover(url: String) {
        Thread {
            var conn: HttpURLConnection? = null
            try {
                conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.connect()
                val raw = BitmapFactory.decodeStream(conn.inputStream)
                if (raw != null) {
                    val target = 1024
                    val scale = min(target.toFloat() / raw.width, target.toFloat() / raw.height)
                    val w = (raw.width * scale).toInt().coerceAtLeast(1)
                    val h = (raw.height * scale).toInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(raw, w, h, true)
                    if (scaled != raw) raw.recycle()
                    pipCoverBitmap = scaled
                    runOnUiThread {
                        pipCoverImg?.setImageBitmap(scaled)
                        updatePipParams()
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { conn?.disconnect() } catch (_: Exception) {}
            }
        }.start()
    }

    private fun wireDownloadCallbacks() {
        DownloadManager.onStatus = { msg ->
            runOnUiThread {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }
        DownloadManager.onProgress = { pct, label ->
            runOnUiThread {
                val safe = label.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ")
                val batch = DownloadManager.isBatchActive()
                webView?.evaluateJavascript(
                    "window.__splDlBatch=$batch;window.splDownloadProgress($pct, '$safe')",
                    null
                )
            }
        }
    }

    private fun startDownloadService() {
        Logger.d(TAG, "starting download service")
        runCatching {
            ContextCompat.startForegroundService(
                this, Intent(this, com.project.lol.service.DownloadService::class.java)
            )
        }
    }

    private fun destroyWebView() {
        Logger.i(TAG, "destroying webview")
        pipVideoView = null
        pipVideoCallback = null
        pipVideoActive.value = false
        hidePipOverlay()
        webView?.let {
            it.stopLoading()
            it.removeJavascriptInterface("AndBridge")
            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_VIEW_RENDERER_TERMINATE)) {
                try {
                    WebViewCompat.getWebViewRenderProcess(it)?.terminate()
                } catch (_: Exception) {}
            }
            // Must detach from its parent before destroy()
            // Otherwise the WebView and its renderer will be left in a bad state;
            (it.parent as? ViewGroup)?.removeView(it)
            it.removeAllViews()
            it.destroy()
        }
        PlayerHost.forget(webView)
        webView = null
        MediaNotificationService.webView = null
    }

    private fun applyOrientation() {
        requestedOrientation = if (landscapeModeState.value) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Logger.d(TAG, "config changed: orientation=${newConfig.orientation} pip=$isInPictureInPictureMode")
        if (isInPictureInPictureMode && pipVideoRequested) {
            setPipFillVideo(true)
            updatePipParams()
        }
    }

    private fun applyKeepScreenOn() {
        if (keepScreenOnState.value) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun startMediaService() {
        Logger.d(TAG, "startMediaService requested")
        if (MediaNotificationService.instance != null) {
            Logger.d(TAG, "media service already running, rebinding webview")
            MediaNotificationService.webView = webView
            return
        }
        if (serviceStarted) return
        serviceStarted = true
        MediaNotificationService.webView = webView
        val intent = Intent(this, MediaNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onStop() {
        super.onStop()
        Logger.i(TAG, "activity stopped: suspending webview loops")
        webView?.evaluateJavascript("""
            try {
                window.__splBg = true;
                if(typeof pfint !== 'undefined' && pfint) { clearInterval(pfint); pfint = null; window.__splWasPfint = true; }
                if(typeof afint !== 'undefined' && afint) { clearInterval(afint); afint = null; window.__splWasAfint = true; }
                if(typeof cssint !== 'undefined' && cssint) { clearInterval(cssint); cssint = null; window.__splWasCssint = true; }
            } catch(e) {}
        """.trimIndent(), null)
        // Server Mode: Android just marked the player hidden; keep the Connect page running at full speed.
        if (ServerMode.isOn(this)) Handler(Looper.getMainLooper()).postDelayed({ PlayerHost.keepVisible() }, 400L)
    }

    override fun onResume() {
        super.onResume()
        Logger.i(TAG, "activity resumed: restoring webview loops")


        prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
        serviceEnabledState.value = prefs.getBoolean("ServiceOn", true)
        materialYouState.value = prefs.getBoolean("MaterialYou", false)
        amoledState.value = prefs.getBoolean("AmoledTheme", false)
        hideTopBarState.value = prefs.getBoolean("HideTopBar", true)
        landscapeModeState.value = prefs.getBoolean("LandscapeMode", false)
        keepScreenOnState.value = prefs.getBoolean("KeepScreenOn", false)
        paletteSeedState.value = prefs.getString("PaletteSeed", null)
        blockServiceWorkerState.value = prefs.getBoolean("BlockServiceWorker", true)
        applyOrientation()
        applyKeepScreenOn()

        val customCss = prefs.getString("CustomCss", "") ?: ""
        val amoledEnabled = prefs.getBoolean("AmoledTheme", false)

        webView?.let { view ->
            view.evaluateJavascript("""
                try {
                    window.__splBg = false;
                    if(window.__splWasPfint) { window.__splWasPfint = false; firstFuck(); }
                    if(window.__splWasAfint) { window.__splWasAfint = false; addAutoFeatures(); }
                    if(window.__splWasCssint) { window.__splWasCssint = false; addCSSJSHack(); }
                    if(window.autoPlayMode === 'onetime') {
                        window.__splApDone = false;
                        window.__splApActive = false;
                        if(typeof splAutoPlay === 'function') splAutoPlay();
                    }
                } catch(e) {}
            """.trimIndent(), null)

            val js = buildString {
                append(buildAmoledJs(amoledEnabled))
                append(AccentTheme.buildAccentJs(this@MainActivity))
                append(LiquidGlassTheme.buildJs(this@MainActivity))
                append(buildCustomCssJs(customCss))
                append(LyricsTheme.buildLyricsStyleJs(prefs.getString("LyricsStyle", LyricsTheme.DEFAULT_STYLE) ?: LyricsTheme.DEFAULT_STYLE))
            }
            view.evaluateJavascript(js, null)

            view.evaluateJavascript(LogoutCheck.CONTENT) { result ->
                if (result == "\"out\"") {
                    prefs.edit().putBoolean("LoggedIn", false).apply()
                    view.loadUrl("https://accounts.spotify.com/login")
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        com.project.lol.util.IncomingLinks.userScriptSource(intent)?.let {
            Logger.i(TAG, "new intent: user script $it")
            pendingScript.value = it
            return
        }
        val link = extractSpotifyLink(intent)
        if (link == null && intent.action == Intent.ACTION_SEND) {
            Toast.makeText(this, "SpotiOS opens Spotify links and user scripts", Toast.LENGTH_SHORT).show()
            return
        }
        Logger.i(TAG, "new intent received: link=${link ?: "none"}")
        if (link == null) {
            val loggedIn = prefs.getBoolean("LoggedIn", false)
            if (!loggedIn) {
                webView?.loadUrl("https://accounts.spotify.com/login")
            }
            return
        }
        if (!prefs.getBoolean("ServiceOn", true)) {
            pendingLink = link
            setServiceEnabled(true)
        } else {
            val wv = webView
            if (wv != null) {
                navigateSpotifyLink(link)
            } else {
                pendingLink = link
            }
        }
    }

    private fun navigateSpotifyLink(link: String) {
        Logger.i(TAG, "navigate to spotify link: $link")
        val wv = webView ?: run {
            pendingLink = link
            return
        }
        val currentHost = wv.url?.let { Uri.parse(it).host }
        val target = Uri.parse(link)
        val path = target.path ?: ""
        val canSpa = currentHost == "open.spotify.com" &&
            target.host == "open.spotify.com" &&
            path.length > 1
        if (!canSpa) {
            Logger.d(TAG, "link needs a full load (spa=false)")
            wv.loadUrl(link)
            return
        }
        Logger.d(TAG, "spa navigation to $path")
        val js = """
            (function() {
                try {
                    var target = ${org.json.JSONObject.quote(path)};
                    if (window.location.pathname === target) return 'same';
                    window.history.pushState({}, '', target);
                    window.dispatchEvent(new PopStateEvent('popstate'));
                    return 'pushed';
                } catch (e) { return 'error:' + e.message; }
            })()
        """.trimIndent()
        wv.evaluateJavascript(js) { result ->
            val status = result?.trim('"') ?: "null"
            if (status != "pushed") {
                wv.loadUrl(link)
                return@evaluateJavascript
            }
            wv.postDelayed({
                if (wv.url == null) return@postDelayed
                wv.evaluateJavascript("window.location.pathname") { check ->
                    val pathname = check?.trim('"') ?: ""
                    if (pathname != path) {
                        wv.loadUrl(link)
                    }
                }
            }, 800)
        }
    }

    override fun onDestroy() {
        if (countedScreen) {
            countedScreen = false
            PlayerHost.screens--
        }
        prefs.unregisterOnSharedPreferenceChangeListener(spoPrefListener)
        cancelSleepTimer()
        pipVideoView = null
        pipVideoCallback = null
        pipVideoActive.value = false
        hidePipOverlay()
        // Server Mode: SpotiOS was swiped away (or closed), but the server keeps playing and
        // stays on Spotify Connect. Hand the player to the media service instead of destroying it.
        val keep = !restarting && webView != null && ServerMode.isOn(this) &&
            MediaNotificationService.instance != null && prefs.getBoolean("ServiceOn", true)
        if (keep && PlayerHost.release(this, keep = true)) {
            Logger.i(TAG, "activity destroyed, server mode keeps the player running")
            webView = null
            serviceStarted = false
            super.onDestroy()
            return
        }
        Logger.i(TAG, "activity destroyed, tearing down webview")
        PlayerHost.forget(webView)
        webView?.let {
            it.stopLoading()
            it.clearHistory()
            it.clearFormData()
            it.removeJavascriptInterface("AndBridge")
            (it.parent as? ViewGroup)?.removeView(it)
            it.removeAllViews()
            it.destroy()
        }
        webView = null
        MediaNotificationService.webView = null
        serviceStarted = false
        super.onDestroy()
    }
}

