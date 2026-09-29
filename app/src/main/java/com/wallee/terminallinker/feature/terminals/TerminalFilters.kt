package com.wallee.terminallinker.feature.terminals

import com.wallee.terminallinker.core.api.dto.PaymentTerminal

/** Segmented filter in the list (docs/03 §Terminalliste). */
enum class TerminalFilter { ALL, UNLINKED, LINKED }

/** Sort menu; UNLINKED_FIRST is the default (unlinked first, then by name). */
enum class TerminalSort { UNLINKED_FIRST, NAME, IDENTIFIER, LOCATION, ACTIVATED }

private val DECOMMISSIONED_STATES = setOf("DECOMMISSIONING", "DECOMMISSIONED")

val PaymentTerminal.isDecommissioned: Boolean get() = state in DECOMMISSIONED_STATES

val PaymentTerminal.locationName: String? get() = locationVersion?.location?.name

val PaymentTerminal.configurationName: String? get() = configurationVersion?.configuration?.name

val PaymentTerminal.displayName: String get() = name?.takeIf { it.isNotBlank() } ?: identifier ?: id.toString()

/** Pure list logic, unit-tested: hide decommissioned, free-text query, segment filter, sort. */
object TerminalFilters {
    fun visible(terminals: List<PaymentTerminal>, showDecommissioned: Boolean): List<PaymentTerminal> =
        if (showDecommissioned) terminals else terminals.filterNot { it.isDecommissioned }

    /** Case-insensitive "contains" over name, identifier, serial number and location name. */
    fun search(terminals: List<PaymentTerminal>, query: String): List<PaymentTerminal> {
        val q = query.trim()
        if (q.isEmpty()) return terminals
        return terminals.filter { t ->
            listOfNotNull(t.name, t.identifier, t.deviceSerialNumber, t.locationName).any {
                it.contains(q, ignoreCase = true)
            }
        }
    }

    fun filter(terminals: List<PaymentTerminal>, filter: TerminalFilter): List<PaymentTerminal> = when (filter) {
        TerminalFilter.ALL -> terminals
        TerminalFilter.UNLINKED -> terminals.filterNot { it.linked }
        TerminalFilter.LINKED -> terminals.filter { it.linked }
    }

    fun sort(terminals: List<PaymentTerminal>, sort: TerminalSort): List<PaymentTerminal> {
        val byName = compareBy<PaymentTerminal, String>(String.CASE_INSENSITIVE_ORDER) { it.displayName }
        return when (sort) {
            TerminalSort.UNLINKED_FIRST -> terminals.sortedWith(compareBy<PaymentTerminal> { it.linked }.then(byName))

            TerminalSort.NAME -> terminals.sortedWith(byName)

            TerminalSort.IDENTIFIER -> terminals.sortedWith(
                compareBy<PaymentTerminal, String?>(nullsLast(String.CASE_INSENSITIVE_ORDER)) {
                    it.identifier
                }.then(byName),
            )

            TerminalSort.LOCATION -> terminals.sortedWith(
                compareBy<PaymentTerminal, String?>(nullsLast(String.CASE_INSENSITIVE_ORDER)) {
                    it.locationName
                }.then(byName),
            )

            TerminalSort.ACTIVATED -> terminals.sortedWith(
                // Newest first, terminals without a date at the end (reverseOrder inside nullsLast keeps nulls last).
                compareBy<PaymentTerminal, String?>(nullsLast(reverseOrder())) {
                    it.activatedOn
                }.then(byName),
            )
        }
    }

    data class Counts(val all: Int, val unlinked: Int, val linked: Int)

    fun counts(terminals: List<PaymentTerminal>): Counts {
        val linked = terminals.count { it.linked }
        return Counts(all = terminals.size, unlinked = terminals.size - linked, linked = linked)
    }
}
