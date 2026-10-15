package com.security.chat.multiplatform.common.core.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class SocketAuthEnvelope(val type: String, val payload: String)

@Serializable
internal data class SocketAuthUpdate(val requestId: String, val accessToken: String)

@Serializable
internal data class SocketAuthState(
    val expiresAt: Long,
    val expiresInMillis: Long,
    val requestId: String? = null,
)

@Serializable
internal data class SocketAuthError(val code: String, val requestId: String? = null)

internal const val SOCKET_AUTH_ID = "socket-auth"
