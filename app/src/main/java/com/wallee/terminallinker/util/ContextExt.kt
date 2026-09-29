package com.wallee.terminallinker.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Walks the context chain to the hosting activity (Compose gives a ContextWrapper). */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
