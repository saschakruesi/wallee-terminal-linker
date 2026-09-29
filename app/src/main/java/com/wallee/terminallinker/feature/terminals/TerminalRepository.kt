package com.wallee.terminallinker.feature.terminals

import com.wallee.terminallinker.core.api.WalleeApiException
import com.wallee.terminallinker.core.api.WalleeClient
import com.wallee.terminallinker.core.api.dto.PaymentTerminal
import com.wallee.terminallinker.core.api.dto.SearchResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Display cache of one space: the last loaded list plus load status (docs/01 §Datenhaltung). */
data class TerminalsState(
    val terminals: List<PaymentTerminal> = emptyList(),
    val loadedAtMillis: Long? = null,
    val loading: Boolean = false,
    val error: Throwable? = null,
    /** True when the cap of [TerminalRepository.MAX_TERMINALS] was reached and the list is incomplete. */
    val capped: Boolean = false,
) {
    val loaded: Boolean get() = loadedAtMillis != null
}

/**
 * Loads all terminals of a space (offset pagination à 100 until `hasMore = false`, cap 1000) and keeps an
 * in-memory cache per space. Actions always reload the affected terminal from the server (docs/02 §3.3).
 */
class TerminalRepository(private val client: WalleeClient, private val clock: () -> Long = System::currentTimeMillis) {
    private val states = mutableMapOf<Long, MutableStateFlow<TerminalsState>>()

    @Volatile
    private var expandSupported = true

    fun state(spaceId: Long): StateFlow<TerminalsState> = flowFor(spaceId)

    fun cached(spaceId: Long, terminalId: Long): PaymentTerminal? =
        flowFor(spaceId).value.terminals.firstOrNull { it.id == terminalId }

    /** Reloads the whole list. Keeps the previous list visible while loading; on failure keeps it and sets [TerminalsState.error]. */
    suspend fun refreshAll(spaceId: Long) {
        val flow = flowFor(spaceId)
        flow.update { it.copy(loading = true, error = null) }
        try {
            val (terminals, capped) = fetchAll(spaceId)
            flow.update { TerminalsState(terminals = terminals, loadedAtMillis = clock(), capped = capped) }
        } catch (e: Exception) {
            flow.update { it.copy(loading = false, error = e) }
            throw e
        }
    }

    /** `GET /payment/terminals/{id}` — the truth after any action. */
    suspend fun get(spaceId: Long, terminalId: Long): PaymentTerminal {
        val terminal: PaymentTerminal = client.get(
            "/payment/terminals/$terminalId",
            expand = expands(),
            spaceId = spaceId,
        )
        put(spaceId, terminal)
        return terminal
    }

    /** `POST /payment/terminals/{id}/refresh` → 200 PaymentTerminal replaces the cached entry. */
    suspend fun refreshDevice(spaceId: Long, terminalId: Long): PaymentTerminal {
        val terminal: PaymentTerminal = client.post(
            "/payment/terminals/$terminalId/refresh",
            expand = expands(),
            spaceId = spaceId,
        )
        put(spaceId, terminal)
        return terminal
    }

    /** `POST /payment/terminals/{id}/trigger-configuration` → 200. */
    suspend fun triggerConfiguration(spaceId: Long, terminalId: Long) {
        client.raw("POST", "/payment/terminals/$terminalId/trigger-configuration", spaceId = spaceId)
    }

    /** `PATCH /payment/terminals/{id}` with `name` and the current `version` (optimistic locking, 409 → reload). */
    suspend fun rename(spaceId: Long, terminal: PaymentTerminal, newName: String): PaymentTerminal {
        val body = JsonObject(
            mapOf(
                "name" to JsonPrimitive(newName),
                "version" to JsonPrimitive(terminal.version ?: 0),
            ),
        ).toString()
        val response = client.raw(
            "PATCH",
            "/payment/terminals/${terminal.id}",
            expand = expands(),
            spaceId = spaceId,
            jsonBody = body,
        )
        val updated: PaymentTerminal = client.decode(response.body)
        put(spaceId, updated)
        return updated
    }

    fun put(spaceId: Long, terminal: PaymentTerminal) {
        flowFor(spaceId).update { state ->
            val list = state.terminals.toMutableList()
            val index = list.indexOfFirst { it.id == terminal.id }
            if (index >= 0) list[index] = terminal else list += terminal
            state.copy(terminals = list)
        }
    }

    fun clear() {
        states.clear()
    }

    private suspend fun fetchAll(spaceId: Long): Pair<List<PaymentTerminal>, Boolean> {
        val all = mutableListOf<PaymentTerminal>()
        var offset = 0
        while (true) {
            val page: SearchResponse<PaymentTerminal> = try {
                client.get(
                    "/payment/terminals/search",
                    query = listOf("limit" to PAGE_SIZE.toString(), "offset" to offset.toString()),
                    expand = expands(),
                    spaceId = spaceId,
                )
            } catch (e: WalleeApiException) {
                // Unknown expand names would be a client error; fall back to plain objects once.
                if (expandSupported && e.status in 400..422 && e.status != 401 && e.status != 403 && e.status != 404) {
                    expandSupported = false
                    continue
                }
                throw e
            }
            all += page.data
            offset += page.data.size
            if (!page.hasMore || page.data.isEmpty()) return all to false
            if (all.size >= MAX_TERMINALS) return all to true
        }
    }

    private fun expands(): List<String> = if (expandSupported) EXPAND else emptyList()

    private fun flowFor(spaceId: Long): MutableStateFlow<TerminalsState> =
        synchronized(states) { states.getOrPut(spaceId) { MutableStateFlow(TerminalsState()) } }

    companion object {
        const val PAGE_SIZE = 100
        const val MAX_TERMINALS = 1000

        /** Nested objects needed for location and configuration names (docs/02 §3.2). */
        val EXPAND = listOf("locationVersion.location", "configurationVersion.configuration")
    }
}
