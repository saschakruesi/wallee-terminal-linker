package com.wallee.terminallinker.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wallee.terminallinker.TerminalLinkerApp

/** The process-wide container, reachable from any composable. */
@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as TerminalLinkerApp).container

/** Creates (or retrieves) a ViewModel scoped to the current nav entry with access to the container. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = appContainer()
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}

/** Like [appViewModel], additionally handing over a [SavedStateHandle] for state that must survive process death. */
@Composable
inline fun <reified VM : ViewModel> appViewModelWithState(
    crossinline create: (AppContainer, SavedStateHandle) -> VM,
): VM {
    val container = appContainer()
    return viewModel(factory = viewModelFactory { initializer { create(container, createSavedStateHandle()) } })
}
