package com.security.chat.multiplatform.features.chat.data.network.entity

public sealed interface ChatEventNM {
    public val id: String
    public val connectionId: Long

    public data class MessageReceived(
        override val id: String,
        override val connectionId: Long,
        val messageId: String,
        val chatId: String,
        val receivedByUserId: String,
    ) : ChatEventNM
}
