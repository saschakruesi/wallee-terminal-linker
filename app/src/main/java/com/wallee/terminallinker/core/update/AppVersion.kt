package com.wallee.terminallinker.core.update

/** SemVer-ish comparison for release tags (`v1.2.0`) against `versionName` (`1.1.0-debug`). */
object AppVersion {
    /** Numeric components of a version string; null when it does not start with a number. */
    fun parse(version: String): List<Int>? {
        val core = version.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
        val parts = core.split('.').map { it.toIntOrNull() ?: return null }
        return parts.takeIf { it.isNotEmpty() }
    }

    /** True when [latestTag] is strictly newer than [currentVersion]; unparsable input is never "newer". */
    fun isNewer(latestTag: String, currentVersion: String): Boolean {
        val latest = parse(latestTag) ?: return false
        val current = parse(currentVersion) ?: return false
        val length = maxOf(latest.size, current.size)
        for (i in 0 until length) {
            val a = latest.getOrElse(i) { 0 }
            val b = current.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}
