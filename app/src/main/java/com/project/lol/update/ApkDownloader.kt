package com.project.lol.update

import android.content.Context
import com.project.lol.BuildConfig
import com.project.lol.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Downloads update APKs into the app's cache (`cache/updates`). */
object ApkDownloader {

    private const val TAG = "update"

    private fun dir(context: Context): File = File(context.cacheDir, "updates")

    fun fileFor(context: Context, update: AppUpdate): File =
        File(dir(context), "SpotiOS-${update.versionName}-${update.build}.apk")

    /** Reuses a complete earlier download of the same release, so Retry or Allow doesn't start over. */
    suspend fun download(
        context: Context,
        update: AppUpdate,
        onProgress: (done: Long, total: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val url = update.apkUrl ?: throw IOException("the release has no APK")
        val target = fileFor(context, update)
        if (update.apkSize > 0 && target.length() == update.apkSize) {
            Logger.i(TAG, "using the APK downloaded earlier: ${target.name}")
            return@withContext target
        }
        clear(context)
        target.parentFile?.mkdirs()
        val part = File(target.path + ".part")

        Logger.i(TAG, "downloading ${update.apkName} (${update.apkSize} bytes)")
        // GitHub answers with a redirect to its file host; HttpURLConnection follows it (https to https).
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("User-Agent", "SpotiOS/${BuildConfig.VERSION_NAME}")
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            val total = update.apkSize.takeIf { it > 0 } ?: conn.contentLengthLong
            var done = 0L
            var reported = 0L
            conn.inputStream.use { input ->
                FileOutputStream(part).use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        done += n
                        if (done - reported >= 128 * 1024) {
                            reported = done
                            onProgress(done, total)
                        }
                    }
                    out.fd.sync()
                }
            }
            onProgress(done, total)
        } catch (e: Exception) {
            part.delete()
            throw e
        } finally {
            conn.disconnect()
        }
        if (!part.renameTo(target)) throw IOException("could not keep the download")
        target
    }

    fun clear(context: Context) {
        dir(context).deleteRecursively()
    }
}
