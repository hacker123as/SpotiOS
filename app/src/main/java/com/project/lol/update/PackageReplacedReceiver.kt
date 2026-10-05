package com.project.lol.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.project.lol.BuildConfig
import com.project.lol.R
import com.project.lol.service.ServerBootWorker
import com.project.lol.service.ServerMode
import com.project.lol.util.Logger

/** Runs in the new version right after SpotiOS was updated. */
class PackageReplacedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "update"
        private const val CHANNEL = "spotios_updates"
        private const val NOTICE_ID = 9
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        Logger.s(TAG, "updated to ${BuildConfig.VERSION_NAME} (build ${BuildConfig.CI_BUILD})")
        ApkDownloader.clear(context)
        UpdateCheckWorker.cancelNotice(context)
        // Installing closed SpotiOS: bring the server back by itself, with a second try in a
        // moment in case Android refused this one.
        if (ServerMode.isOn(context)) {
            ServerMode.startInBackground(context, "updated")
            ServerBootWorker.enqueue(context, "updated")
        }
        UpdateManager.takeInstalledVersion(context)?.let { showUpdated(context) }
    }

    /** The app closed itself to update, so say it worked. */
    private fun showUpdated(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.update_channel), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { setShowBadge(false) }
        )
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val pi = PendingIntent.getActivity(context, NOTICE_ID, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.update_done_title, BuildConfig.VERSION_NAME))
            .setContentText(context.getString(R.string.update_done_text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(NOTICE_ID, n) }
    }
}
