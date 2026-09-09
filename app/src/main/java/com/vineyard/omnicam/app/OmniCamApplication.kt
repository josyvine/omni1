package com.vineyard.omnicam.app

import android.app.Application
import com.vineyard.omnicam.app.core.crash.OmniCrashHandler
import com.vineyard.omnicam.app.di.AppModule

class OmniCamApplication : Application() {

    lateinit var appModule: AppModule
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Install Crash Handler to save uncaught crashes to /sdcard/omni log/
        OmniCrashHandler.install(this)
        appModule = AppModule(this)
    }

    companion object {
        lateinit var instance: OmniCamApplication
            private set
    }
}
