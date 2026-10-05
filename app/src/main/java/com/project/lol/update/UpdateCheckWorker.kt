package com.project.lol.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.project.lol.R
import com.project.lol.ui.MainActivity
import com.project.lol.util.Logger
import com.project.lol.util.UpdateChecker
import java.util.concurrent.TimeUnit

/**
 * Every few hours, even when SpotiOS isn't open: asks GitHub Releases for a newer SpotiOS (the
 * same check as at app start) and says so in a notification, once per build. Tapping it opens
 * SpotiOS with [EXTRA_CHECK_UPDATE], which checks again and shows the update card.
 */
class UpdateCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val ctx = applicationContext
        if (!canNotify(ctx)) {
            Logger.v(TAG, "notifications are off, skipping the background update check")
            return Result.success()
        }
        val checker = UpdateChecker(ctx)
        val update = (checker.checkBlocking() as? UpdateChecker.Result.Available)?.update
            ?: return Result.success()
        if (checker.isPutOff(update)) {
            Logger.i(TAG, "${update.tag} put off with Later, no notification")
            return Result.success()
        }
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastBuild = prefs.getInt(KEY_NOTIFIED_BUILD, 0)
        val lastTag = prefs.getString(KEY_NOTIFIED_TAG, null)
        if (!UpdateNotifyRule.shouldNotify(update, lastBuild, lastTag)) {
            Logger.v(TAG, "already told about ${update.tag}")
            return Result.success()
        }
        if (showNotice(ctx, update)) {
            Logger.i(TAG, "notified about ${update.tag}")
            prefs.edit()
                .putInt(KEY_NOTIFIED_BUILD, update.build)
                .putString(KEY_NOTIFIED_TAG, update.tag)
                .apply()
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "update"
        private const val NAME = "spotios-update-check"
        private const val PREFS = "spotilol_prefs"
        /** The CI build (and tag) the last "is ready" notification was about. */
        private const val KEY_NOTIFIED_BUILD = "UpdateNotifiedBuild"
        private const val KEY_NOTIFIED_TAG = "UpdateNotifiedTag"
        private const val CHANNEL = "spotios_update_ready"
        private const val NOTICE_ID = 10
        private const val INTERVAL_HOURS = 3L

        /** On the intent that opens MainActivity: check for an update now and show the card. */
        const val EXTRA_CHECK_UPDATE = "com.project.lol.CHECK_UPDATE"

        fun schedule(context: Context) {
            runCatching {
                val req = PeriodicWorkRequest.Builder(UpdateCheckWorker::class.java, INTERVAL_HOURS, TimeUnit.HOURS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
            }.onFailure { Logger.e(TAG, "couldn't schedule the background update check", it) }
        }

        fun cancelNotice(context: Context) {
            runCatching { context.getSystemService(NotificationManager::class.java)?.cancel(NOTICE_ID) }
        }

        /** Notifications allowed (POST_NOTIFICATIONS on Android 13+, and not turned off for SpotiOS). */
        private fun canNotify(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return false
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        private fun showNotice(context: Context, update: AppUpdate): Boolean {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return false
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.update_ready_channel), NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { setShowBadge(true) }
            )
            val open = Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_CHECK_UPDATE, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pi = PendingIntent.getActivity(context, NOTICE_ID, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.update_ready_title, update.versionName))
                .setContentText(context.getString(R.string.update_ready_text))
                .setContentIntent(pi)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .build()
            return runCatching { nm.notify(NOTICE_ID, n) }.isSuccess
        }
    }
}

/** Whether a newer build is worth a notification: once per CI build, which only goes up. */
object UpdateNotifyRule {
    fun shouldNotify(update: AppUpdate, lastBuild: Int, lastTag: String?): Boolean =
        if (update.build > 0) update.build > lastBuild else update.tag != lastTag
}
