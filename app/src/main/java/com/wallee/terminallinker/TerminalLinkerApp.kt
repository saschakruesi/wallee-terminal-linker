package com.wallee.terminallinker

import android.app.Application
import com.wallee.terminallinker.di.AppContainer

class TerminalLinkerApp : Application() {
    /** Replaceable so instrumented tests can point the app at a mock server. */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
