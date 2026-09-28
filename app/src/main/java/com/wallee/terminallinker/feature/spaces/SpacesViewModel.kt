package com.wallee.terminallinker.feature.spaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallee.terminallinker.core.api.toUserMessage
import com.wallee.terminallinker.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddSpaceState(
    val open: Boolean = false,
    val id: String = "",
    val checking: Boolean = false,
    val verified: SpaceRef? = null,
    val error: String? = null,
)

/** Space list, active space and the "add space ID" dialog — shared by header chip, sheet and settings. */
class SpacesViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.spaceRepository

    val spaces: StateFlow<List<SpaceRef>> = repo.spaces.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val recent: StateFlow<List<SpaceRef>> = repo.recentSpaces.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val active: StateFlow<SpaceRef?> = repo.activeSpace.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )
    val manual: StateFlow<List<SpaceRef>> = repo.manualSpaces.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val mode: StateFlow<SpaceMode> = repo.mode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SpaceMode.AUTO,
    )
    val truncated: StateFlow<Boolean> =
        repo.discoveryTruncated.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _addSpace = MutableStateFlow(AddSpaceState())
    val addSpace: StateFlow<AddSpaceState> = _addSpace

    fun select(id: Long) {
        viewModelScope.launch { repo.setActive(id) }
    }

    fun openAddSpace() = _addSpace.update { AddSpaceState(open = true) }

    fun closeAddSpace() = _addSpace.update { AddSpaceState() }

    fun onAddSpaceIdChange(value: String) = _addSpace.update {
        it.copy(id = value.filter(Char::isDigit), verified = null, error = null)
    }

    fun verifyAddSpace() {
        val id = _addSpace.value.id.toLongOrNull() ?: return
        _addSpace.update { it.copy(checking = true, error = null, verified = null) }
        viewModelScope.launch {
            try {
                val space = repo.verify(id)
                _addSpace.update { it.copy(checking = false, verified = space) }
            } catch (e: Exception) {
                _addSpace.update { it.copy(checking = false, error = e.toUserMessage(container.appContext)) }
            }
        }
    }

    fun confirmAddSpace(activate: Boolean = true) {
        val space = _addSpace.value.verified ?: return
        viewModelScope.launch {
            repo.addManual(space)
            if (activate) repo.setActive(space.id)
            _addSpace.value = AddSpaceState()
        }
    }

    /** Space IDs handed over from the credential scanner (spaces-only mode): verify and add each. */
    fun consumeScannedSpaceIds() {
        val scanned = container.credentialHandoff.take() ?: return
        viewModelScope.launch {
            var first: SpaceRef? = null
            scanned.spaceIds.forEach { id ->
                runCatching { repo.verify(id) }.getOrNull()?.let { space ->
                    repo.addManual(space)
                    if (first == null) first = space
                }
            }
            first?.let { repo.setActive(it.id) }
            _addSpace.value = AddSpaceState()
        }
    }

    fun removeManual(id: Long) {
        viewModelScope.launch { repo.removeManual(id) }
    }
}
