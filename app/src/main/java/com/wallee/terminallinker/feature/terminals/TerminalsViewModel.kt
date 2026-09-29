package com.wallee.terminallinker.feature.terminals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallee.terminallinker.core.api.dto.PaymentTerminal
import com.wallee.terminallinker.core.api.toUserMessage
import com.wallee.terminallinker.di.AppContainer
import com.wallee.terminallinker.feature.spaces.SpaceRef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TerminalsUiState(
    val space: SpaceRef? = null,
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val capped: Boolean = false,
    val loadedAtMillis: Long? = null,
    val query: String = "",
    val filter: TerminalFilter = TerminalFilter.ALL,
    val sort: TerminalSort = TerminalSort.UNLINKED_FIRST,
    val counts: TerminalFilters.Counts = TerminalFilters.Counts(0, 0, 0),
    /** Visible rows after hide-decommissioned, search, filter and sort. */
    val rows: List<PaymentTerminal> = emptyList(),
)

/** Terminal list of the active space: loads on space change, filters and sorts locally (docs/03 §Terminalliste). */
@OptIn(ExperimentalCoroutinesApi::class)
class TerminalsViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.terminalRepository
    private val query = MutableStateFlow("")
    private val activeSpace = container.spaceRepository.activeSpace.distinctUntilChanged()

    private val repoState = activeSpace.flatMapLatest { space ->
        if (space == null) flowOf(TerminalsState()) else repo.state(space.id)
    }

    private val listPrefs = combine(
        container.uiPrefs.terminalFilter,
        container.uiPrefs.terminalSort,
        container.uiPrefs.showDecommissioned,
    ) { filter, sort, showDecommissioned -> Triple(filter, sort, showDecommissioned) }

    val state: StateFlow<TerminalsUiState> = combine(activeSpace, repoState, listPrefs, query) {
            space,
            repoState,
            prefs,
            query,
        ->
        val (filter, sort, showDecommissioned) = prefs
        val visible = TerminalFilters.visible(repoState.terminals, showDecommissioned)
        val searched = TerminalFilters.search(visible, query)
        TerminalsUiState(
            space = space,
            loading = repoState.loading,
            loaded = repoState.loaded,
            error = repoState.error?.toUserMessage(container.appContext),
            capped = repoState.capped,
            loadedAtMillis = repoState.loadedAtMillis,
            query = query,
            filter = filter,
            sort = sort,
            counts = TerminalFilters.counts(searched),
            rows = TerminalFilters.sort(TerminalFilters.filter(searched, filter), sort),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TerminalsUiState())

    init {
        // Load once per space when nothing is cached yet; pull-to-refresh reloads explicitly.
        viewModelScope.launch {
            activeSpace.collect { space ->
                if (space != null && !repo.state(space.id).value.loaded && !repo.state(space.id).value.loading) {
                    runCatching { repo.refreshAll(space.id) }
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun clearQuery() = onQueryChange("")

    fun setFilter(filter: TerminalFilter) {
        viewModelScope.launch { container.uiPrefs.setTerminalFilter(filter) }
    }

    fun setSort(sort: TerminalSort) {
        viewModelScope.launch { container.uiPrefs.setTerminalSort(sort) }
    }

    fun refresh() {
        val space = state.value.space ?: return
        viewModelScope.launch { runCatching { repo.refreshAll(space.id) } }
    }
}
