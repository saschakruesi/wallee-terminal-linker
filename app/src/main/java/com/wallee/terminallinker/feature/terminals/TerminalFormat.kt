package com.wallee.terminallinker.feature.terminals

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val TIME = DateTimeFormatter.ofPattern("HH:mm")

/** wallee dates are ISO-8601 with offset; shown as dd.MM.yyyy in the device zone. */
fun formatWalleeDate(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(DATE) }
        .getOrElse { iso.take(10) }
}

fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(TIME)
