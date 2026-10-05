package com.project.lol

import android.app.Application
import com.project.lol.util.CrashHandler
import com.project.lol.util.Logger

class SpotilolApp : Application() {

    companion object {
        /** The application context, for code that runs without a screen (Server Mode). */
        @Volatile var context: android.content.Context? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        Logger.init(this)
        CrashHandler.install(this)
        Logger.s("app", "started")
    }
}
