package com.security.chat.multiplatform.common.core.db

internal fun getDbPasswordKey(databaseName: String): String {
    return KEY_DB_PASSWORD + "_" + databaseName
}

private const val KEY_DB_PASSWORD = "KEY_DB_PASSWORD"