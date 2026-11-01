package com.security.chat.multiplatform.features.chat.data.network.entity

public sealed interface ChatEventNM {
    public val id: String
    public val connectionId: Long
    public val messageId: String
    public val chatId: String

    public data class MessageReceived(
        override val id: String,
        override val connectionId: Long,
        override val messageId: String,
        override val chatId: String,
        val receivedByUserId: String,
    ) : ChatEventNM

    public data class MessageRead(
        override val id: String,
        override val connectionId: Long,
        override val messageId: String,
        override val chatId: String,
        val readByUserId: String,
    ) : ChatEventNM
}
