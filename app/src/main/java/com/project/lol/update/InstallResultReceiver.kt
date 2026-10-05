package com.project.lol.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** PackageInstaller's answer to an install session: needs a confirmation, failed, or done. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        UpdateManager.onInstallStatus(context, intent)
    }
}
