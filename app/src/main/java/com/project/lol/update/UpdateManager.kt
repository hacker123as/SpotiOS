package com.project.lol.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import androidx.core.content.IntentCompat
import com.project.lol.util.Logger
import com.project.lol.util.UpdateChecker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * The in-app update, from "SpotiOS X is available" to Android installing it over the running app.
 * The update card ([com.project.lol.ui.components.UpdatePrompt]) shows [state].
 */
object UpdateManager {

    private const val TAG = "update"
    private const val PREFS = "spotilol_prefs"
    /** The version SpotiOS is installing over itself; read once the update is in. */
    private const val KEY_INSTALLING = "UpdateInstalling"

    enum class Problem { DOWNLOAD, DAMAGED, NOT_SPOTIOS, INSTALL }

    sealed interface State {
        val update: AppUpdate?

        data object Hidden : State {
            override val update: AppUpdate? get() = null
        }

        data class Available(override val update: AppUpdate) : State
        data class Downloading(override val update: AppUpdate, val done: Long, val total: Long) : State
        /** Waiting for "Install unknown apps" to be allowed for SpotiOS. */
        data class NeedsPermission(override val update: AppUpdate, val apk: File) : State
        /** [confirm] is Android's install prompt, launched by the screen while it is set. */
        data class Installing(override val update: AppUpdate, val confirm: Intent? = null) : State
        data class Failed(override val update: AppUpdate, val problem: Problem, val detail: String? = null) : State
        /** Signed with another key: saved to Downloads as [savedAs] (null if that failed) for a one-time reinstall. */
        data class Reinstall(override val update: AppUpdate, val savedAs: String?) : State
    }

    private val _state = MutableStateFlow<State>(State.Hidden)
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    /** From the check at app start. */
    fun offer(update: AppUpdate) {
        _state.compareAndSet(State.Hidden, State.Available(update))
    }

    /** From Settings > Check for updates. Leaves a running download or install alone. */
    fun show(update: AppUpdate) {
        _state.update { if (it is State.Downloading || it is State.Installing) it else State.Available(update) }
    }

    fun later(context: Context) {
        _state.value.update?.let { UpdateChecker(context).putOff(it) }
        close()
    }

    fun close() {
        job?.cancel()
        job = null
        _state.value = State.Hidden
    }

    /** Update (or Retry): download, check the signer, then install or explain the reinstall. */
    fun start(context: Context) {
        val update = _state.value.update ?: return
        if (update.apkUrl == null) {
            openInBrowser(context, update)
            close()
            return
        }
        val app = context.applicationContext
        job?.cancel()
        _state.value = State.Downloading(update, 0, update.apkSize)
        job = scope.launch {
            val apk = try {
                ApkDownloader.download(app, update) { done, total ->
                    _state.update { if (it is State.Downloading && it.update == update) State.Downloading(update, done, total) else it }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(TAG, "download failed: ${e.message}")
                moveOn(update, State.Failed(update, Problem.DOWNLOAD, e.message))
                return@launch
            }
            if (update.apkSize > 0 && apk.length() != update.apkSize) {
                Logger.w(TAG, "download size ${apk.length()} != ${update.apkSize}")
                apk.delete()
                moveOn(update, State.Failed(update, Problem.DAMAGED))
                return@launch
            }
            when (ApkInstaller.checkSigner(app, apk)) {
                ApkInstaller.Signer.UNREADABLE -> {
                    apk.delete()
                    moveOn(update, State.Failed(update, Problem.DAMAGED))
                }
                ApkInstaller.Signer.NOT_SPOTIOS -> moveOn(update, State.Failed(update, Problem.NOT_SPOTIOS))
                ApkInstaller.Signer.OTHER -> reinstall(app, update, apk)
                ApkInstaller.Signer.SAME, ApkInstaller.Signer.UNKNOWN -> install(app, update, apk)
            }
        }
    }

    /** Only moves on from the download this job started; Cancel or Later may have closed it. */
    private fun moveOn(update: AppUpdate, next: State) {
        _state.update { if (it is State.Downloading && it.update == update) next else it }
    }

    private fun install(context: Context, update: AppUpdate, apk: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            Logger.i(TAG, "waiting for Install unknown apps")
            moveOn(update, State.NeedsPermission(update, apk))
            return
        }
        val from = _state.value
        if ((from !is State.Downloading && from !is State.NeedsPermission) || from.update != update) return
        if (!_state.compareAndSet(from, State.Installing(update))) return
        try {
            // commit(), not apply(): Android may stop SpotiOS right after the session is committed.
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_INSTALLING, update.versionName).commit()
            ApkInstaller.install(context, apk)
        } catch (e: Exception) {
            Logger.e(TAG, "install session failed", e)
            clearInstalling(context)
            _state.value = State.Failed(update, Problem.INSTALL, e.message)
        }
    }

    private fun reinstall(context: Context, update: AppUpdate, apk: File) {
        val saved = ApkInstaller.saveToDownloads(context, apk, update.apkName ?: apk.name)
        Logger.w(TAG, "${update.tag} is signed with another key; saved to Downloads as $saved")
        _state.update { if (it.update == update && it !is State.Hidden) State.Reinstall(update, saved) else it }
    }

    /** The screen is back, maybe from Android's "Install unknown apps" page. */
    fun onResume(context: Context) {
        val waiting = _state.value as? State.NeedsPermission ?: return
        if (!context.packageManager.canRequestPackageInstalls()) return
        val app = context.applicationContext
        job = scope.launch { install(app, waiting.update, waiting.apk) }
    }

    /** The screen showed Android's install prompt. */
    fun confirmShown() {
        _state.update { if (it is State.Installing && it.confirm != null) it.copy(confirm = null) else it }
    }

    fun onInstallStatus(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        Logger.i(TAG, "install status $status ${message.orEmpty()}")
        val update = _state.value.update
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java) ?: return
                if (update != null) {
                    _state.value = State.Installing(update, confirm)
                } else {
                    runCatching { context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                clearInstalling(context)
                _state.value = State.Hidden
            }
            else -> {
                clearInstalling(context)
                if (update == null) return
                when {
                    // Cancelled in Android's prompt.
                    status == PackageInstaller.STATUS_FAILURE_ABORTED -> _state.value = State.Available(update)
                    status == PackageInstaller.STATUS_FAILURE_CONFLICT && message?.contains("signature", ignoreCase = true) == true -> {
                        val app = context.applicationContext
                        job = scope.launch { reinstall(app, update, ApkDownloader.fileFor(app, update)) }
                    }
                    else -> _state.value = State.Failed(update, Problem.INSTALL, message)
                }
            }
        }
    }

    /** The version SpotiOS just installed over itself, given out once. */
    fun takeInstalledVersion(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val version = prefs.getString(KEY_INSTALLING, null) ?: return null
        prefs.edit().remove(KEY_INSTALLING).apply()
        return version
    }

    private fun clearInstalling(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_INSTALLING).apply()
    }

    /** Download in browser: the APK link itself, or the release page. */
    fun openInBrowser(context: Context, update: AppUpdate?) {
        val url = update?.apkUrl ?: update?.pageUrl?.takeIf { it.isNotBlank() } ?: UpdateChecker.RELEASES_URL
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
