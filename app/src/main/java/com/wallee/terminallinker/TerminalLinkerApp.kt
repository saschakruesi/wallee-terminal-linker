package com.wallee.terminallinker

import android.app.Application
import com.wallee.terminallinker.di.AppContainer

class TerminalLinkerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
