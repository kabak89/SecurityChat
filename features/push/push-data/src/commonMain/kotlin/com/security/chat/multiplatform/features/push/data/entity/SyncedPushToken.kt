package com.security.chat.multiplatform.features.push.data.entity

internal data class SyncedPushToken(
    val userId: String,
    val token: String,
)