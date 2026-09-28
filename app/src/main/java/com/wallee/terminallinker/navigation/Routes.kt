package com.wallee.terminallinker.navigation

import kotlinx.serialization.Serializable

/** Type-safe routes (docs/03 §Navigation). Never put secrets (auth key, raw QR payload) into a route. */
@Serializable
object SetupRoute

@Serializable
object TerminalsRoute

@Serializable
data class TerminalDetailRoute(val terminalId: Long)

enum class ScanMode { LINK, REPLACE, CREDENTIALS }

@Serializable
data class ScanRoute(val mode: ScanMode, val terminalId: Long? = null, val spacesOnly: Boolean = false)

enum class ResultOutcome { LINKED, REPLACED, UNLINKED }

@Serializable
data class ResultRoute(
    val terminalId: Long,
    val outcome: ResultOutcome,
    val serial: String? = null,
    val previousSerial: String? = null,
)

@Serializable
object SettingsRoute

/** Debug-only component reference (docs/04 §Styleguide-Screen). */
@Serializable
object StyleguideRoute
