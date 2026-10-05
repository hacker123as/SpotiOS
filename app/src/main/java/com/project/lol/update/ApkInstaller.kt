package com.project.lol.update

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import com.project.lol.util.Logger
import java.io.File
import java.security.MessageDigest

/** Checks a downloaded APK and installs it over the running app with Android's PackageInstaller. */
object ApkInstaller {

    private const val TAG = "update"
    private const val APK_MIME = "application/vnd.android.package-archive"

    enum class Signer {
        /** Signed with the installed app's key: installs in place. */
        SAME,
        /** Signed with another key: Android only installs it after an uninstall. */
        OTHER,
        NOT_SPOTIOS,
        /** Not a readable APK. */
        UNREADABLE,
        /** Readable but without certificates we can compare; the installer decides. */
        UNKNOWN
    }

    @Suppress("DEPRECATION")
    fun checkSigner(context: Context, apk: File): Signer {
        val pm = context.packageManager
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val archive = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(apk.path, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                pm.getPackageArchiveInfo(apk.path, flags)
            }
        }.getOrNull() ?: return Signer.UNREADABLE
        if (archive.packageName != context.packageName) return Signer.NOT_SPOTIOS
        val installed = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                pm.getPackageInfo(context.packageName, flags)
            }
        }.getOrNull()
        val theirs = archive.signingInfo ?: return Signer.UNKNOWN
        val ours = digests(installed?.signingInfo?.apkContentsSigners)
        if (ours.isEmpty()) return Signer.UNKNOWN
        val same = ours == digests(theirs.apkContentsSigners) ||
            // After a key rotation the new APK carries the old key in its history and still installs.
            (!theirs.hasMultipleSigners() && digests(theirs.signingCertificateHistory).containsAll(ours))
        Logger.i(TAG, "update signer ${if (same) "matches" else "differs from"} the installed app")
        return if (same) Signer.SAME else Signer.OTHER
    }

    private fun digests(signatures: Array<Signature>?): Set<String> =
        signatures.orEmpty().map { sig ->
            MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()

    /** Installs [apk] over SpotiOS. The result arrives in [InstallResultReceiver]. */
    fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        installer.mySessions.forEach { runCatching { installer.abandonSession(it.sessionId) } }

        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                apk.inputStream().use { input ->
                    session.openWrite("base.apk", 0, apk.length()).use { out ->
                        input.copyTo(out, 64 * 1024)
                        session.fsync(out)
                    }
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                val callback = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    Intent(context, InstallResultReceiver::class.java),
                    flags
                )
                Logger.i(TAG, "committing install session $sessionId")
                session.commit(callback.intentSender)
            }
        } catch (e: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            throw e
        }
    }

    /**
     * Copies [apk] to the public Downloads folder as [name] for a manual reinstall.
     * Returns the name it got (Android may add a number), or null if it couldn't be saved.
     */
    fun saveToDownloads(context: Context, apk: File, name: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            // Replace a copy saved earlier instead of piling up "SpotiOS (1).apk" files.
            runCatching {
                resolver.delete(collection, "${MediaStore.MediaColumns.DISPLAY_NAME}=?", arrayOf(name))
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, APK_MIME)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: error("MediaStore refused the file")
            try {
                val out = resolver.openOutputStream(uri) ?: error("could not open $uri")
                out.use { apk.inputStream().use { input -> input.copyTo(it) } }
                resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                runCatching { resolver.delete(uri, null, null) }
                throw e
            }
            resolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: name
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            apk.copyTo(File(dir, name), overwrite = true).name
        }
    } catch (e: Exception) {
        Logger.w(TAG, "could not save the APK to Downloads: ${e.message}")
        null
    }

    fun unknownSourcesIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    fun uninstallIntent(context: Context): Intent =
        Intent(Intent.ACTION_DELETE, Uri.parse("package:${context.packageName}"))

    fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
}
