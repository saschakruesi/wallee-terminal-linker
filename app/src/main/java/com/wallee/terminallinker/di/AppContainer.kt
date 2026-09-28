package com.wallee.terminallinker.di

import android.content.Context

/**
 * Manual dependency container (docs/01 §DI). Phase 1 has nothing to wire yet; phase 2 adds the wallee
 * client, credential store, preferences and repositories here.
 */
class AppContainer(@Suppress("unused") private val context: Context)
