package com.project.lol.service

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.project.lol.util.Logger
import java.util.concurrent.TimeUnit

/**
 * Starts Server Mode a little after the phone started or SpotiOS was updated. Android 15+ refuses
 * a mediaPlayback foreground service started from a BOOT_COMPLETED receiver (or while that
 * broadcast's allowance lasts), so the start waits [DELAY_SECONDS]. If Android still refuses it
 * (battery optimization on), ServerMode shows its "Tap to start" notification instead.
 */
class ServerBootWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val ctx = applicationContext
        val reason = inputData.getString(KEY_REASON) ?: "boot"
        if (!ServerMode.isOn(ctx)) return Result.success()
        if (MediaNotificationService.instance != null) {
            Logger.i(TAG, "server already running ($reason)")
            return Result.success()
        }
        ServerMode.startInBackground(ctx, reason, deferDuringBoot = false)
        return Result.success()
    }

    companion object {
        private const val TAG = "server-boot"
        private const val NAME = "spotios-server-boot"
        private const val KEY_REASON = "reason"
        /** Longer than the ~20 s allowance Android gives an app after BOOT_COMPLETED. */
        private const val DELAY_SECONDS = 30L

        /** [replace] restarts the wait (a new boot); otherwise a start already waiting is kept. */
        fun enqueue(context: Context, reason: String, replace: Boolean = false) {
            runCatching {
                val req = OneTimeWorkRequest.Builder(ServerBootWorker::class.java)
                    .setInitialDelay(DELAY_SECONDS, TimeUnit.SECONDS)
                    .setInputData(Data.Builder().putString(KEY_REASON, reason).build())
                    .build()
                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    NAME,
                    if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                    req
                )
                Logger.i(TAG, "server start in ${DELAY_SECONDS}s ($reason)")
            }.onFailure { Logger.e(TAG, "couldn't schedule the server start", it) }
        }
    }
}
