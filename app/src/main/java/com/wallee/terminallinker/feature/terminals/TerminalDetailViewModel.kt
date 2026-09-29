package com.wallee.terminallinker.feature.terminals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.api.WalleeApiException
import com.wallee.terminallinker.core.api.dto.PaymentTerminal
import com.wallee.terminallinker.core.api.toUserMessage
import com.wallee.terminallinker.core.ui.components.WToastKind
import com.wallee.terminallinker.di.AppContainer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TerminalDetailUiState(
    val spaceId: Long? = null,
    val terminal: PaymentTerminal? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val busy: Boolean = false,
    val renaming: Boolean = false,
    val renameValue: String = "",
    val confirmConfiguration: Boolean = false,
)

data class ToastEvent(val message: String, val kind: WToastKind = WToastKind.Success)

/** Detail actions from docs/03 §Terminal-Detail: rename, refresh, trigger configuration, copy ID. */
class TerminalDetailViewModel(private val container: AppContainer, private val terminalId: Long) : ViewModel() {
    private val repo = container.terminalRepository
    private val _state = MutableStateFlow(TerminalDetailUiState())
    val state: StateFlow<TerminalDetailUiState> = _state

    private val _toasts = MutableSharedFlow<ToastEvent>(extraBufferCapacity = 4)
    val toasts: SharedFlow<ToastEvent> = _toasts

    init {
        viewModelScope.launch {
            val spaceId = container.spaceRepository.activeSpace.first()?.id
            _state.update { it.copy(spaceId = spaceId) }
            if (spaceId == null) {
                _state.update {
                    it.copy(loading = false, error = container.appContext.getString(R.string.space_choose_first))
                }
                return@launch
            }
            repo.cached(spaceId, terminalId)?.let { cached ->
                _state.update { it.copy(terminal = cached, loading = false) }
            }
            load(spaceId)
        }
    }

    private suspend fun load(spaceId: Long) {
        try {
            val fresh = repo.get(spaceId, terminalId)
            _state.update { it.copy(terminal = fresh, loading = false, error = null) }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    loading = false,
                    error = if (it.terminal ==
                        null
                    ) {
                        e.toUserMessage(container.appContext)
                    } else {
                        null
                    },
                )
            }
            if (_state.value.terminal != null) toast(e.toUserMessage(container.appContext), WToastKind.Error)
        }
    }

    fun refresh() = action { spaceId ->
        repo.refreshDevice(spaceId, terminalId).also { fresh -> _state.update { it.copy(terminal = fresh) } }
        toast(container.appContext.getString(R.string.detail_refreshed))
    }

    fun askTriggerConfiguration() = _state.update { it.copy(confirmConfiguration = true) }

    fun dismissTriggerConfiguration() = _state.update { it.copy(confirmConfiguration = false) }

    fun triggerConfiguration() {
        _state.update { it.copy(confirmConfiguration = false) }
        action { spaceId ->
            repo.triggerConfiguration(spaceId, terminalId)
            toast(container.appContext.getString(R.string.detail_config_triggered))
        }
    }

    fun startRename() = _state.update { it.copy(renaming = true, renameValue = it.terminal?.name.orEmpty()) }

    fun onRenameChange(value: String) = _state.update { it.copy(renameValue = value) }

    fun cancelRename() = _state.update { it.copy(renaming = false) }

    fun saveRename() {
        val terminal = _state.value.terminal ?: return
        val newName = _state.value.renameValue.trim()
        if (newName.isEmpty() || newName == terminal.name) {
            cancelRename()
            return
        }
        action { spaceId ->
            try {
                val updated = repo.rename(spaceId, terminal, newName)
                _state.update { it.copy(terminal = updated, renaming = false) }
                toast(container.appContext.getString(R.string.detail_renamed))
            } catch (e: WalleeApiException) {
                if (e.status == 409) {
                    load(spaceId)
                    _state.update { it.copy(renaming = false) }
                    toast(container.appContext.getString(R.string.detail_conflict_reloaded), WToastKind.Error)
                } else {
                    throw e
                }
            }
        }
    }

    fun onIdCopied() = toast(container.appContext.getString(R.string.detail_id_copied))

    private fun action(block: suspend (spaceId: Long) -> Unit) {
        val spaceId = _state.value.spaceId ?: return
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                block(spaceId)
            } catch (e: Exception) {
                toast(e.toUserMessage(container.appContext), WToastKind.Error)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private fun toast(message: String, kind: WToastKind = WToastKind.Success) {
        _toasts.tryEmit(ToastEvent(message, kind))
    }
}
