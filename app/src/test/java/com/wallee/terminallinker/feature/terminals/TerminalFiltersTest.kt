package com.wallee.terminallinker.feature.terminals

import com.wallee.terminallinker.core.api.dto.NamedRef
import com.wallee.terminallinker.core.api.dto.PaymentTerminal
import com.wallee.terminallinker.core.api.dto.PaymentTerminalLocationVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TerminalFiltersTest {
    private fun t(
        id: Long,
        name: String,
        identifier: String? = null,
        serial: String? = null,
        location: String? = null,
        state: String = "ACTIVE",
        activatedOn: String? = null,
    ) = PaymentTerminal(
        id = id,
        name = name,
        identifier = identifier,
        state = state,
        deviceSerialNumber = serial,
        locationVersion = location?.let {
            PaymentTerminalLocationVersion(id = id * 10, location = NamedRef(id * 100, it))
        },
        activatedOn = activatedOn,
    )

    private val kasse1 = t(1, "Kasse 1", "WT-8F3K2", location = "Filiale Winterthur")
    private val kasse2 =
        t(
            2,
            "Kasse 2",
            "WT-8F3K3",
            serial = "2290012345",
            location = "Filiale Winterthur",
            activatedOn = "2026-03-12T10:00:00Z",
        )
    private val bar = t(3, "Bar Terminal", "WT-8F3K9", location = "Filiale Zürich", state = "INACTIVE")
    private val old =
        t(4, "Altes Gerät", "WT-0000", serial = "1", state = "DECOMMISSIONED", activatedOn = "2024-01-01T00:00:00Z")
    private val alpha = t(5, "alpha", "WT-A", serial = "9", location = "Aarau", activatedOn = "2026-09-01T00:00:00Z")
    private val all = listOf(kasse1, kasse2, bar, old, alpha)

    @Test
    fun `decommissioned terminals are hidden unless requested`() {
        assertEquals(listOf(1L, 2L, 3L, 5L), TerminalFilters.visible(all, showDecommissioned = false).map { it.id })
        assertEquals(5, TerminalFilters.visible(all, showDecommissioned = true).size)
    }

    @Test
    fun `search matches name, identifier, serial and location case-insensitively`() {
        assertEquals(listOf(1L, 2L), TerminalFilters.search(all, "kasse").map { it.id })
        assertEquals(listOf(3L), TerminalFilters.search(all, "8f3k9").map { it.id })
        assertEquals(listOf(2L), TerminalFilters.search(all, "2290012345").map { it.id })
        assertEquals(listOf(3L), TerminalFilters.search(all, "zürich").map { it.id })
        assertEquals(all, TerminalFilters.search(all, "   "))
        assertEquals(emptyList<PaymentTerminal>(), TerminalFilters.search(all, "xyz"))
    }

    @Test
    fun `segment filter uses the serial number as the link criterion`() {
        assertEquals(listOf(2L, 4L, 5L), TerminalFilters.filter(all, TerminalFilter.LINKED).map { it.id })
        assertEquals(listOf(1L, 3L), TerminalFilters.filter(all, TerminalFilter.UNLINKED).map { it.id })
        assertEquals(TerminalFilters.Counts(all = 5, unlinked = 2, linked = 3), TerminalFilters.counts(all))
    }

    @Test
    fun `unlinked first then name, case-insensitive`() {
        val sorted = TerminalFilters.sort(all, TerminalSort.UNLINKED_FIRST)
        assertEquals(listOf("Bar Terminal", "Kasse 1", "alpha", "Altes Gerät", "Kasse 2"), sorted.map { it.name })
    }

    @Test
    fun `other sort orders`() {
        assertEquals(
            listOf("alpha", "Altes Gerät", "Bar Terminal", "Kasse 1", "Kasse 2"),
            TerminalFilters.sort(all, TerminalSort.NAME).map {
                it.name
            },
        )
        assertEquals(
            listOf("WT-0000", "WT-8F3K2", "WT-8F3K3", "WT-8F3K9", "WT-A"),
            TerminalFilters.sort(all, TerminalSort.IDENTIFIER).map {
                it.identifier
            },
        )
        assertEquals(
            listOf("Aarau", "Filiale Winterthur", "Filiale Winterthur", "Filiale Zürich", null),
            TerminalFilters.sort(all, TerminalSort.LOCATION).map {
                it.locationName
            },
        )
        assertEquals(listOf(5L, 2L, 4L, 3L, 1L), TerminalFilters.sort(all, TerminalSort.ACTIVATED).map { it.id })
    }
}
