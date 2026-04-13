package com.security.chat.multiplatform.common.core.db

import com.security.chat.multiplatform.common.core.db.utils.isPlainDatabaseFileName
import java.nio.file.Files
import java.nio.file.Paths

internal fun desktopJdbcSqliteUrl(databaseName: String): String {
    require(isPlainDatabaseFileName(databaseName)) {
        "databaseName must be a single file name, got: $databaseName"
    }
    val dbFile = Paths.get(System.getProperty("user.home"))
        .resolve(".SecurityChat")
        .resolve("databases")
        .resolve(databaseName)
    Files.createDirectories(dbFile.parent)
    val absolute = dbFile.toAbsolutePath().normalize()
    return "jdbc:sqlite:$absolute"
}
