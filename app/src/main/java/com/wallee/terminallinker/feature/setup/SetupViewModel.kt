package com.wallee.terminallinker.feature.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallee.terminallinker.core.api.toUserMessage
import com.wallee.terminallinker.core.auth.Credentials
import com.wallee.terminallinker.core.auth.ScannedCredentials
import com.wallee.terminallinker.di.AppContainer
import com.wallee.terminallinker.feature.spaces.DiscoveryResult
import com.wallee.terminallinker.feature.spaces.SpaceRef
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupUiState(
    val userId: String = "",
    val key: String = "",
    /** A key is stored and the field shows the mask; the user must tap "ändern" to type a new one. */
    val keyStored: Boolean = false,
    val remember: Boolean = true,
    val label: String? = null,
    val testing: Boolean = false,
    val error: String? = null,
    val foundCount: Int? = null,
    /** Shown when discovery returned no spaces (docs/03 Setup, "Keine Spaces gefunden"). */
    val manualPanel: Boolean = false,
    val manualForbidden: Boolean = false,
    val manualId: String = "",
    val manualChecking: Boolean = false,
    val manualVerified: SpaceRef? = null,
    val manualError: String? = null,
    /** Pending scanned credentials awaiting "replace existing?" confirmation. */
    val replaceDialog: ScannedCredentials? = null,
) {
    val canSubmit: Boolean get() = !testing && userId.isNotBlank() && (key.isNotBlank() || keyStored)
}

/** Credentials form and connection test (docs/03 §Setup, docs/02 §3.1). Keys never leave this class unmasked. */
class SetupViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state

    private val _done = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val done: SharedFlow<Unit> = _done

    private var storedCredentials: Credentials? = container.credentialStore.load()
    private var preferredSpaceIds: List<Long> = emptyList()

    init {
        storedCredentials?.let { existing ->
            _state.update {
                it.copy(
                    userId = existing.userId.toString(),
                    keyStored = true,
                    remember = container.credentialStore.isPersisted(),
                )
            }
        }
        consumeHandoff()
    }

    fun onUserIdChange(value: String) = _state.update { it.copy(userId = value.filter(Char::isDigit), error = null) }

    fun onKeyChange(value: String) = _state.update { it.copy(key = value.trim(), error = null) }

    fun onRememberChange(value: Boolean) = _state.update { it.copy(remember = value) }

    fun onEditStoredKey() = _state.update { it.copy(keyStored = false, key = "") }

    fun onManualIdChange(value: String) = _state.update {
        it.copy(manualId = value.filter(Char::isDigit), manualVerified = null, manualError = null)
    }

    /** Called when the screen (re)appears: picks up credentials handed over by the QR scanner. */
    fun consumeHandoff() {
        val scanned = container.credentialHandoff.take() ?: return
        if (storedCredentials != null && storedCredentials?.userId != scanned.userId) {
            _state.update { it.copy(replaceDialog = scanned) }
        } else {
            applyScanned(scanned)
        }
    }

    fun onReplaceConfirmed() {
        val scanned = _state.value.replaceDialog ?: return
        _state.update { it.copy(replaceDialog = null) }
        applyScanned(scanned)
    }

    fun onReplaceDismissed() = _state.update { it.copy(replaceDialog = null) }

    private fun applyScanned(scanned: ScannedCredentials) {
        preferredSpaceIds = scanned.spaceIds
        _state.update {
            it.copy(
                userId = scanned.userId.toString(),
                key = scanned.key,
                keyStored = false,
                label = scanned.label,
                error = null,
            )
        }
        testAndSave()
    }

    fun testAndSave() {
        val current = _state.value
        val userId = current.userId.toLongOrNull()
        if (userId == null || userId <= 0) {
            _state.update {
                it.copy(
                    error = container.appContext.getString(com.wallee.terminallinker.R.string.setup_validation_user_id),
                )
            }
            return
        }
        val key = when {
            current.key.isNotBlank() -> current.key
            current.keyStored -> storedCredentials?.authenticationKey
            else -> null
        }
        if (key.isNullOrBlank()) {
            _state.update {
                it.copy(error = container.appContext.getString(com.wallee.terminallinker.R.string.setup_validation_key))
            }
            return
        }
        val previous = storedCredentials
        val credentials = Credentials(userId, key)
        _state.update { it.copy(testing = true, error = null, foundCount = null, manualPanel = false) }
        viewModelScope.launch {
            container.credentialStore.save(credentials, persist = current.remember)
            try {
                when (val result = container.spaceRepository.discover()) {
                    is DiscoveryResult.Found -> {
                        storedCredentials = credentials
                        _state.update {
                            it.copy(testing = false, foundCount = result.spaces.size, key = "", keyStored = true)
                        }
                        addPreferredSpaces(result.spaces)
                        container.spaceRepository.chooseActive(preferredSpaceIds.firstOrNull())
                        _done.tryEmit(Unit)
                    }

                    is DiscoveryResult.None -> {
                        storedCredentials = credentials
                        _state.update {
                            it.copy(
                                testing = false,
                                manualPanel = true,
                                manualForbidden = result.forbidden,
                                key = "",
                                keyStored = true,
                            )
                        }
                        if (preferredSpaceIds.isNotEmpty()) {
                            addPreferredSpaces(emptyList())
                            if (container.spaceRepository.chooseActive(preferredSpaceIds.firstOrNull()) !=
                                null
                            ) {
                                _done.tryEmit(Unit)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Do not keep credentials that failed the test: restore the previous set (or none).
                if (previous !=
                    null
                ) {
                    container.credentialStore.save(previous, persist = container.credentialStore.isPersisted())
                } else {
                    container.credentialStore.clear()
                }
                _state.update { it.copy(testing = false, error = e.toUserMessage(container.appContext)) }
            }
        }
    }

    /** Space IDs from a QR code that are not in the discovered list are verified and added as manual spaces. */
    private suspend fun addPreferredSpaces(discovered: List<SpaceRef>) {
        val known = discovered.map { it.id }.toSet()
        preferredSpaceIds.filter { it !in known }.forEach { id ->
            runCatching {
                container.spaceRepository.verify(id)
            }.getOrNull()?.let { container.spaceRepository.addManual(it) }
        }
    }

    fun verifyManual() {
        val id = _state.value.manualId.toLongOrNull() ?: return
        _state.update { it.copy(manualChecking = true, manualError = null, manualVerified = null) }
        viewModelScope.launch {
            try {
                val space = container.spaceRepository.verify(id)
                _state.update { it.copy(manualChecking = false, manualVerified = space) }
            } catch (e: Exception) {
                _state.update { it.copy(manualChecking = false, manualError = e.toUserMessage(container.appContext)) }
            }
        }
    }

    fun addManualAndContinue() {
        val space = _state.value.manualVerified ?: return
        viewModelScope.launch {
            container.spaceRepository.addManual(space)
            container.spaceRepository.setActive(space.id)
            _done.tryEmit(Unit)
        }
    }
}
