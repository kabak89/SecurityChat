package com.security.chat.multiplatform.common.permission.entity

public sealed interface AllowanceResult {

    public data object Allowed : AllowanceResult

    public data object Restricted : AllowanceResult
}
