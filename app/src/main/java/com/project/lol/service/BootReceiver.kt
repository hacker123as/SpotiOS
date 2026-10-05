package com.project.lol.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * The phone finished starting (after the first unlock; not direct-boot aware): bring Server Mode
 * back. The work is in [ServerMode.onBoot].
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val booted = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> true
            // Any app can send this one (the receiver is exported for it), so only right after a start.
            ACTION_QUICKBOOT_POWERON -> SystemClock.elapsedRealtime() < QUICKBOOT_MAX_UPTIME_MS
            else -> false
        }
        if (booted) ServerMode.onBoot(context.applicationContext)
    }

    companion object {
        /** Sent instead of BOOT_COMPLETED by some phones' fast boot. */
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        private const val QUICKBOOT_MAX_UPTIME_MS = 10 * 60_000L
    }
}
