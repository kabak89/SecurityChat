package com.security.chat.multiplatform.common.core.db

import com.security.chat.multiplatform.common.core.db.utils.isPlainDatabaseFileName
import org.sqlite.mc.SQLiteMCSqlCipherConfig
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties

/**
 * JDBC [Properties] for SQLCipher 4.x (aligned with Zetetic SQLCipher on Android), using
 * [io.github.willena:sqlite-jdbc](https://github.com/Willena/sqlite-jdbc-crypt) (SQLite3 Multiple Ciphers).
 */
internal fun securedDesktopJdbcProperties(passphrase: String): Properties {
    val properties = SQLiteMCSqlCipherConfig.getV4Defaults()
        .withKey(passphrase)
        .build()
        .toProperties()
    properties["foreign_keys"] = "true"
    return properties
}

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