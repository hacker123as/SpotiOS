package com.project.lol.service

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.project.lol.util.Logger
import java.util.concurrent.TimeUnit

/**
 * Server Mode safety net: every 15 minutes, if Server Mode is on but Android stopped the
 * SpotiOS service (memory pressure, a battery saver), start it again. Android only allows that
 * from the background when battery optimization is off for SpotiOS; otherwise ServerMode shows
 * a "Tap to start" notification instead.
 */
class ServerWatchdog(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val ctx = applicationContext
        if (!ServerMode.isOn(ctx)) {
            cancel(ctx)
            return Result.success()
        }
        if (MediaNotificationService.instance == null) {
            Logger.w(TAG, "server service isn't running, starting it again")
            ServerMode.startInBackground(ctx, "watchdog")
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "server-watchdog"
        private const val NAME = "spotios-server-watchdog"

        fun schedule(context: Context) {
            runCatching {
                val req = PeriodicWorkRequest.Builder(ServerWatchdog::class.java, 15, TimeUnit.MINUTES).build()
                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
            }.onFailure { Logger.e(TAG, "couldn't schedule the watchdog", it) }
        }

        fun cancel(context: Context) {
            runCatching { WorkManager.getInstance(context.applicationContext).cancelUniqueWork(NAME) }
        }
    }
}
