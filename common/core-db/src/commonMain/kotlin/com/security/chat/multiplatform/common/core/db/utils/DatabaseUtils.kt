package com.security.chat.multiplatform.common.core.db.utils

internal fun isPlainDatabaseFileName(name: String): Boolean {
    if (name.isEmpty() || name == "." || name == "..") return false
    if (name.contains('/') || name.contains('\\') || name.contains("..")) return false
    return true
}