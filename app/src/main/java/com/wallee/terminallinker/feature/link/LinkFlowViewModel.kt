package com.wallee.terminallinker.feature.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallee.terminallinker.R
import com.wallee.terminallinker.core.api.WalleeNetworkException
import com.wallee.terminallinker.core.api.dto.PaymentTerminal
import com.wallee.terminallinker.core.api.toUserMessage
import com.wallee.terminallinker.core.serial.SerialNumber
import com.wallee.terminallinker.di.AppContainer
import com.wallee.terminallinker.navigation.ResultOutcome
import com.wallee.terminallinker.navigation.ScanMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where a multi-step action failed (docs/03 §Fehler- und Teilfehlerzustände). */
enum class FailedPhase {
    /** Replace step 1 failed: nothing changed. */
    UNLINK_BEFORE_REPLACE,

    /** Replace step 2 failed after a successful unlink: the terminal is now without a device. */
    LINK_AFTER_UNLINK,

    /** Transport failure during an action: the server state is unknown until reloaded. */
    NETWORK_UNCLEAR,
}

sealed class LinkFlowState {
    data object Scanning : LinkFlowState()

    /** Serial recognised; [error] holds the wallee message when a link attempt was rejected (422/409). */
    data class Confirm(val serial: String, val error: String? = null) : LinkFlowState()

    data class Invalid(val raw: String) : LinkFlowState()

    /** [step] 1-based, [totalSteps] 1 for link, 2 for replace. */
    data class Working(val step: Int, val totalSteps: Int) : LinkFlowState()

    data class Done(
        val outcome: ResultOutcome,
        val serial: String,
        val previousSerial: String?,
        val confirmed: Boolean,
    ) : LinkFlowState()

    data class Failed(val phase: FailedPhase, val message: String, val serial: String, val previousSerial: String?) :
        LinkFlowState()
}

data class LinkFlowUiState(
    val terminal: PaymentTerminal? = null,
    val spaceId: Long? = null,
    val state: LinkFlowState = LinkFlowState.Scanning,
    /** After a successful unlink inside a replace, only the link step is repeated (docs/02 §3.3). */
    val unlinkDone: Boolean = false,
)

/**
 * Link / replace state machine: Scanning → Confirm(serial) → Working(step) → Done | Failed (docs/05 Phase 4).
 * After every 204 the terminal is reloaded; "linked" is only shown when the server says so.
 */
class LinkFlowViewModel(
    private val container: AppContainer,
    private val terminalId: Long,
    private val mode: ScanMode,
) : ViewModel() {
    private val repo = container.terminalRepository
    private val _ui = MutableStateFlow(LinkFlowUiState())
    val ui: StateFlow<LinkFlowUiState> = _ui

    init {
        viewModelScope.launch {
            val spaceId = container.spaceRepository.activeSpace.first()?.id
            val cached = spaceId?.let { repo.cached(it, terminalId) }
            _ui.update { it.copy(spaceId = spaceId, terminal = cached) }
            if (spaceId != null && cached == null) {
                runCatching {
                    repo.get(spaceId, terminalId)
                }.getOrNull()?.let { fresh -> _ui.update { it.copy(terminal = fresh) } }
            }
        }
    }

    /** Raw value from the camera or manual entry. Ignored unless the flow is waiting for a scan. */
    fun onScanned(raw: String) {
        if (_ui.value.state != LinkFlowState.Scanning) return
        SerialNumber.parse(raw).fold(
            onSuccess = { serial -> _ui.update { it.copy(state = LinkFlowState.Confirm(serial)) } },
            onFailure = { _ui.update { it.copy(state = LinkFlowState.Invalid(raw.take(80))) } },
        )
    }

    fun rescan() = _ui.update { it.copy(state = LinkFlowState.Scanning) }

    fun confirm() {
        val confirm = _ui.value.state as? LinkFlowState.Confirm ?: return
        val spaceId = _ui.value.spaceId ?: return
        val previousSerial = _ui.value.terminal?.deviceSerialNumber
        val replace = mode == ScanMode.REPLACE && !_ui.value.unlinkDone
        viewModelScope.launch {
            if (replace) {
                _ui.update { it.copy(state = LinkFlowState.Working(1, 2)) }
                try {
                    repo.unlink(spaceId, terminalId)
                } catch (e: Exception) {
                    _ui.update {
                        it.copy(state = failure(e, FailedPhase.UNLINK_BEFORE_REPLACE, confirm.serial, previousSerial))
                    }
                    return@launch
                }
                _ui.update { it.copy(unlinkDone = true, state = LinkFlowState.Working(2, 2)) }
            } else {
                _ui.update {
                    it.copy(
                        state = LinkFlowState.Working(
                            if (_ui.value.unlinkDone) 2 else 1,
                            if (mode ==
                                ScanMode.REPLACE
                            ) {
                                2
                            } else {
                                1
                            },
                        ),
                    )
                }
            }
            link(spaceId, confirm.serial, previousSerial)
        }
    }

    private suspend fun link(spaceId: Long, serial: String, previousSerial: String?) {
        try {
            repo.link(spaceId, terminalId, serial)
        } catch (e: WalleeNetworkException) {
            _ui.update {
                it.copy(
                    state = LinkFlowState.Failed(
                        FailedPhase.NETWORK_UNCLEAR,
                        e.toUserMessage(container.appContext),
                        serial,
                        previousSerial,
                    ),
                )
            }
            return
        } catch (e: Exception) {
            val message = e.toUserMessage(container.appContext)
            _ui.update {
                it.copy(
                    state = if (it.unlinkDone) {
                        LinkFlowState.Failed(
                            FailedPhase.LINK_AFTER_UNLINK,
                            message,
                            serial,
                            previousSerial,
                        )
                    } else {
                        LinkFlowState.Confirm(serial, message)
                    },
                )
            }
            return
        }
        finish(spaceId, serial, previousSerial)
    }

    private suspend fun finish(spaceId: Long, serial: String, previousSerial: String?) {
        val fresh = runCatching { repo.get(spaceId, terminalId) }.getOrNull()
        val confirmed = fresh?.deviceSerialNumber?.equals(serial, ignoreCase = true) == true
        val outcome = if (_ui.value.unlinkDone ||
            mode == ScanMode.REPLACE
        ) {
            ResultOutcome.REPLACED
        } else {
            ResultOutcome.LINKED
        }
        _ui.update {
            it.copy(
                terminal = fresh ?: it.terminal,
                state = LinkFlowState.Done(outcome, serial, previousSerial, confirmed),
            )
        }
    }

    /** Replace step 1 failed: nothing changed, run the whole replace again with the same serial. */
    fun relinkOrRetry() {
        val failed = _ui.value.state as? LinkFlowState.Failed ?: return
        _ui.update { it.copy(state = LinkFlowState.Confirm(failed.serial)) }
        confirm()
    }

    /** Replace step 2 failed: scan again and repeat only the link. */
    fun retryLinkOnly() = rescan()

    /** Replace step 2 failed: put the old device back. */
    fun relinkOld() {
        val failed = _ui.value.state as? LinkFlowState.Failed ?: return
        val old = failed.previousSerial ?: return
        _ui.update { it.copy(state = LinkFlowState.Confirm(old)) }
        confirm()
    }

    /** After a network failure: reload and decide from the server state. */
    fun checkState() {
        val failed = _ui.value.state as? LinkFlowState.Failed ?: return
        val spaceId = _ui.value.spaceId ?: return
        viewModelScope.launch {
            _ui.update { it.copy(state = LinkFlowState.Working(1, 1)) }
            val fresh = runCatching { repo.get(spaceId, terminalId) }.getOrNull()
            when {
                fresh == null -> _ui.update { it.copy(state = failed) }

                fresh.deviceSerialNumber.equals(failed.serial, ignoreCase = true) ->
                    _ui.update {
                        it.copy(
                            terminal = fresh,
                            state = LinkFlowState.Done(
                                if (mode ==
                                    ScanMode.REPLACE
                                ) {
                                    ResultOutcome.REPLACED
                                } else {
                                    ResultOutcome.LINKED
                                },
                                failed.serial,
                                failed.previousSerial,
                                true,
                            ),
                        )
                    }

                else -> _ui.update {
                    it.copy(
                        terminal = fresh,
                        unlinkDone = it.unlinkDone || !fresh.linked,
                        state = LinkFlowState.Confirm(failed.serial),
                    )
                }
            }
        }
    }

    private fun failure(e: Exception, phase: FailedPhase, serial: String, previousSerial: String?): LinkFlowState =
        LinkFlowState.Failed(
            phase = if (e is WalleeNetworkException) FailedPhase.NETWORK_UNCLEAR else phase,
            message = e.toUserMessage(container.appContext),
            serial = serial,
            previousSerial = previousSerial,
        )

    @Suppress("unused")
    private val unusedStringRef = R.string.app_name
}
