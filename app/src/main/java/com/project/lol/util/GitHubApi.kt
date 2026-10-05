package com.project.lol.util

import android.os.Handler
import android.os.Looper
import com.project.lol.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class GitHubAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long
)

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val htmlUrl: String,
    val assets: List<GitHubAsset> = emptyList()
) {
    /** The first .apk attached to the release. */
    val apk: GitHubAsset?
        get() = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
}

sealed interface ReleaseResult {
    data class Found(val release: GitHubRelease) : ReleaseResult
    /** GitHub allows 60 calls an hour per IP without a token. */
    data object RateLimited : ReleaseResult
    /** [code] is the HTTP status, or -1 when GitHub could not be reached. */
    data class Failed(val code: Int) : ReleaseResult
}

object GitHubApi {

    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "GitHubApi-Worker").apply { isDaemon = true }
        }

    fun fetchLatestRelease(
        owner: String,
        repo: String,
        onResult: (GitHubRelease?) -> Unit
    ) {
        fetchLatestReleaseResult(owner, repo) { result ->
            onResult((result as? ReleaseResult.Found)?.release)
        }
    }

    fun fetchLatestReleaseResult(
        owner: String,
        repo: String,
        onResult: (ReleaseResult) -> Unit
    ) {
        executor.execute {
            val result = latestRelease(owner, repo)
            Handler(Looper.getMainLooper()).post { onResult(result) }
        }
    }

    /** Blocking: the newest release that isn't a draft or prerelease. */
    fun latestRelease(owner: String, repo: String): ReleaseResult {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL("https://api.github.com/repos/$owner/$repo/releases/latest").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            conn.setRequestProperty("User-Agent", "SpotiOS/${BuildConfig.VERSION_NAME}")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            when (val code = conn.responseCode) {
                200 -> ReleaseResult.Found(
                    parseRelease(JSONObject(conn.inputStream.bufferedReader().readText()))
                )
                403, 429 -> ReleaseResult.RateLimited
                else -> ReleaseResult.Failed(code)
            }
        } catch (_: Exception) {
            ReleaseResult.Failed(-1)
        } finally {
            conn?.disconnect()
        }
    }

    fun parseRelease(json: JSONObject): GitHubRelease {
        val assets = json.optJSONArray("assets")
        return GitHubRelease(
            tagName = json.text("tag_name"),
            name = json.text("name"),
            body = json.text("body"),
            publishedAt = json.text("published_at"),
            htmlUrl = json.text("html_url"),
            assets = (0 until (assets?.length() ?: 0)).mapNotNull { i ->
                val a = assets?.optJSONObject(i) ?: return@mapNotNull null
                GitHubAsset(
                    name = a.text("name"),
                    downloadUrl = a.text("browser_download_url"),
                    size = a.optLong("size", 0L)
                )
            }
        )
    }

    // GitHub sends null for an empty body; Android's optString would turn that into "null".
    private fun JSONObject.text(key: String): String = if (isNull(key)) "" else optString(key, "")
}
