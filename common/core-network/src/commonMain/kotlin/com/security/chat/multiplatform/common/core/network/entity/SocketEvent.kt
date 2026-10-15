package com.security.chat.multiplatform.common.core.network.entity

public sealed interface SocketEvent<out T> {
    public data class Message<T>(val connectionId: Long, val value: T) : SocketEvent<T>
    public data object Disconnected : SocketEvent<Nothing>
}
