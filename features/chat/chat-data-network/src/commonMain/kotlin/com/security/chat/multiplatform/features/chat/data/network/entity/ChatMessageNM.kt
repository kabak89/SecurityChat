package com.security.chat.multiplatform.features.chat.data.network.entity

public sealed interface ChatMessageNM {

    public val id: String
    public val key: String
    public val authorId: String
    public val timestamp: Long
    public val replyToMessageId: String?

    public data class Text(
        override val id: String,
        override val key: String,
        override val authorId: String,
        override val timestamp: Long,
        val text: String,
        override val replyToMessageId: String? = null,
    ) : ChatMessageNM

    public data class Image(
        override val id: String,
        override val key: String,
        override val authorId: String,
        override val timestamp: Long,
        val fileId: String,
        override val replyToMessageId: String? = null,
    ) : ChatMessageNM
}
