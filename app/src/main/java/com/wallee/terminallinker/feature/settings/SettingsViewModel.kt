package com.wallee.terminallinker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallee.terminallinker.core.prefs.AppLanguage
import com.wallee.terminallinker.core.update.UpdateInfo
import com.wallee.terminallinker.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Outcome of an explicit "check for updates" tap. */
sealed class UpdateCheckResult {
    data class Available(val update: UpdateInfo) : UpdateCheckResult()

    data object UpToDate : UpdateCheckResult()

    data object Failed : UpdateCheckResult()
}

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val checkingUpdate: Boolean = false,
    val updateResult: UpdateCheckResult? = null,
    val wiping: Boolean = false,
)

/** Language, update check and "delete all local data" (docs/03 §Einstellungen). */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState(language = container.localePrefs.language.value))
    val state: StateFlow<SettingsUiState> = _state

    val lastUpdateCheckMillis: StateFlow<Long?> =
        container.uiPrefs.lastUpdateCheckMillis.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setLanguage(language: AppLanguage) {
        container.localePrefs.setLanguage(language)
        _state.update { it.copy(language = language) }
    }

    fun checkForUpdates() {
        if (_state.value.checkingUpdate) return
        _state.update { it.copy(checkingUpdate = true, updateResult = null) }
        viewModelScope.launch {
            val before = container.uiPrefs.lastUpdateCheckMillis.first()
            val update = runCatching { container.updateChecker.check(force = true) }.getOrNull()
            val after = container.uiPrefs.lastUpdateCheckMillis.first()
            val result = when {
                update != null -> UpdateCheckResult.Available(update)

                // The checker only stores a timestamp after a successful response.
                after != null && after != before -> UpdateCheckResult.UpToDate

                else -> UpdateCheckResult.Failed
            }
            _state.update { it.copy(checkingUpdate = false, updateResult = result) }
        }
    }

    /** Wipes credentials, spaces, prefs and caches; [onDone] runs afterwards on the main thread. */
    fun wipe(onDone: () -> Unit) {
        if (_state.value.wiping) return
        _state.update { it.copy(wiping = true) }
        viewModelScope.launch {
            container.wipe()
            _state.update { it.copy(wiping = false) }
            onDone()
        }
    }
}
