package com.project.lol.util

import android.content.Context
import com.project.lol.BuildConfig
import com.project.lol.update.AppUpdate
import com.project.lol.update.ReleaseTag

/** Looks for a newer SpotiOS on GitHub Releases, the only place updates come from. */
class UpdateChecker(context: Context) {

    companion object {
        private const val TAG = "update"
        const val OWNER = "hacker123as"
        const val REPO = "SpotiOS"
        const val RELEASES_URL = "https://github.com/$OWNER/$REPO/releases/latest"
        private const val PREFS_NAME = "spotilol_prefs"
        private const val KEY_LAST_CHECK = "LastUpdateCheck"
        private const val KEY_LATER_TAG = "UpdateLaterTag"
        private const val KEY_LATER_UNTIL = "UpdateLaterUntil"
        private const val CHECK_INTERVAL_MS = 30 * 60 * 1000L
        private const val LATER_MS = 24 * 60 * 60 * 1000L
    }

    sealed interface Result {
        data class Available(val update: AppUpdate) : Result
        data object UpToDate : Result
        data object RateLimited : Result
        data object Failed : Result
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** At app start: at most every 30 minutes, silent on errors, and skips a release put off with Later. */
    fun autoCheck(onUpdateAvailable: (AppUpdate) -> Unit) {
        val since = System.currentTimeMillis() - prefs.getLong(KEY_LAST_CHECK, 0)
        if (since in 0 until CHECK_INTERVAL_MS) {
            Logger.v(TAG, "update check throttled (last ${since / 1000}s ago)")
            return
        }
        check { result ->
            if (result is Result.Available) {
                if (isPutOff(result.update)) {
                    Logger.i(TAG, "${result.update.tag} put off with Later")
                } else {
                    onUpdateAvailable(result.update)
                }
            }
        }
    }

    /** Settings > Check for updates: always asks GitHub and ignores Later. */
    fun checkNow(onResult: (Result) -> Unit) = check(onResult)

    /** Later: don't offer this release again for a day. A newer one is still offered. */
    fun putOff(update: AppUpdate) {
        prefs.edit()
            .putString(KEY_LATER_TAG, update.tag)
            .putLong(KEY_LATER_UNTIL, System.currentTimeMillis() + LATER_MS)
            .apply()
    }

    fun isPutOff(update: AppUpdate): Boolean =
        prefs.getString(KEY_LATER_TAG, null) == update.tag &&
            System.currentTimeMillis() < prefs.getLong(KEY_LATER_UNTIL, 0)

    /**
     * The background check (UpdateCheckWorker): blocking, and it leaves the app-start throttle
     * alone so opening SpotiOS still shows the update card.
     */
    fun checkBlocking(): Result {
        Logger.i(TAG, "background update check ($OWNER/$REPO)")
        return toResult(GitHubApi.latestRelease(OWNER, REPO))
    }

    private fun check(onResult: (Result) -> Unit) {
        Logger.i(TAG, "checking for updates ($OWNER/$REPO)")
        GitHubApi.fetchLatestReleaseResult(OWNER, REPO) { response ->
            val result = toResult(response)
            // Only an unreachable GitHub is retried on the next start.
            if (response !is ReleaseResult.Failed || response.code > 0) {
                prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
            }
            onResult(result)
        }
    }

    private fun toResult(response: ReleaseResult): Result {
        val result = when (response) {
            is ReleaseResult.Found -> evaluate(response.release)
            ReleaseResult.RateLimited -> Result.RateLimited
            is ReleaseResult.Failed -> Result.Failed
        }
        when (result) {
            is Result.Available -> Logger.s(TAG, "update available: ${result.update.tag}")
            Result.RateLimited -> Logger.w(TAG, "GitHub rate limit hit, will try again later")
            Result.Failed -> Logger.w(TAG, "update check failed ($response)")
            Result.UpToDate -> Unit
        }
        return result
    }

    private fun evaluate(release: GitHubRelease): Result {
        val latest = ReleaseTag.parse(release.tagName) ?: return Result.Failed
        val newer = ReleaseTag.isNewer(latest, BuildConfig.VERSION_NAME, BuildConfig.CI_BUILD)
        Logger.i(
            TAG,
            "latest=${release.tagName} current=${BuildConfig.VERSION_NAME} build=${BuildConfig.CI_BUILD} newer=$newer"
        )
        if (!newer) return Result.UpToDate
        return AppUpdate.from(release)?.let { Result.Available(it) } ?: Result.Failed
    }
}
