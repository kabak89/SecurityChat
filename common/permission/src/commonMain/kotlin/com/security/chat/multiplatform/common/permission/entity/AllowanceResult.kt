package com.security.chat.multiplatform.common.permission.entity

public sealed interface AllowanceResult {

    public data object Allowed : AllowanceResult

    public data class Restricted(
        val isPermanent: Boolean,
    ) : AllowanceResult
}