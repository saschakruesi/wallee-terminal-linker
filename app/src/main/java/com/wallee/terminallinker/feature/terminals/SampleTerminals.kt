package com.wallee.terminallinker.feature.terminals

/**
 * Phase-1 placeholder data so the list, detail and link flows can be navigated before the wallee client
 * exists. Replaced by `TerminalRepository` in phase 3.
 */
data class SampleTerminal(
    val id: Long,
    val name: String,
    val identifier: String,
    val state: String,
    val deviceName: String?,
    val deviceSerialNumber: String?,
    val location: String,
    val configuration: String,
    val type: String,
    val currency: String,
    val activatedOn: String?,
) {
    val linked: Boolean get() = !deviceSerialNumber.isNullOrBlank()
}

object SampleTerminals {
    const val SPACE_NAME = "Hotel Muster – Rezeption"
    const val SPACE_ID = 12345L

    val items: List<SampleTerminal> = listOf(
        SampleTerminal(
            id = 1001,
            name = "Kasse 1",
            identifier = "WT-8F3K2",
            state = "ACTIVE",
            deviceName = "PAX A77",
            deviceSerialNumber = null,
            location = "Filiale Winterthur",
            configuration = "Standard CHF",
            type = "PAX A77",
            currency = "CHF",
            activatedOn = null,
        ),
        SampleTerminal(
            id = 1002,
            name = "Kasse 2",
            identifier = "WT-8F3K3",
            state = "ACTIVE",
            deviceName = "PAX A77",
            deviceSerialNumber = "2290012345",
            location = "Filiale Winterthur",
            configuration = "Standard CHF",
            type = "PAX A77",
            currency = "CHF",
            activatedOn = "12.03.2026",
        ),
        SampleTerminal(
            id = 1003,
            name = "Bar Terminal",
            identifier = "WT-8F3K9",
            state = "INACTIVE",
            deviceName = null,
            deviceSerialNumber = null,
            location = "Filiale Zürich",
            configuration = "Standard CHF",
            type = "PAX A35",
            currency = "CHF",
            activatedOn = null,
        ),
        SampleTerminal(
            id = 1004,
            name = "Rezeption",
            identifier = "WT-8F4A1",
            state = "ACTIVE",
            deviceName = "PAX A920",
            deviceSerialNumber = "2290067890",
            location = "Hotel Muster",
            configuration = "Hotel EUR/CHF",
            type = "PAX A920",
            currency = "CHF",
            activatedOn = "02.09.2026",
        ),
    )

    fun byId(id: Long): SampleTerminal? = items.firstOrNull { it.id == id }
}
