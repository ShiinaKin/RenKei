package io.sakurasou.renkei.db

import java.nio.file.Files
import java.nio.file.Path

internal fun prepareSQLiteDatabasePath(jdbcURL: String) {
    sqliteDatabaseFile(jdbcURL)
        ?.parent
        ?.let(Files::createDirectories)
}

internal fun sqliteDatabaseFile(jdbcURL: String): Path? {
    if (!jdbcURL.startsWith(SQLITE_JDBC_PREFIX, ignoreCase = true)) return null

    val location = jdbcURL.substring(SQLITE_JDBC_PREFIX.length)
    if (location.isBlank() || location.isInMemorySQLiteLocation()) return null

    return location
        .removePrefix("file:")
        .substringBefore('?')
        .takeIf { it.isNotBlank() }
        ?.let(Path::of)
        ?.toAbsolutePath()
        ?.normalize()
}

private fun String.isInMemorySQLiteLocation(): Boolean =
    equals(":memory:", ignoreCase = true) ||
        startsWith("file::memory:", ignoreCase = true) ||
        substringAfter('?', missingDelimiterValue = "")
            .split('&')
            .any { it.equals("mode=memory", ignoreCase = true) }

private const val SQLITE_JDBC_PREFIX = "jdbc:sqlite:"
