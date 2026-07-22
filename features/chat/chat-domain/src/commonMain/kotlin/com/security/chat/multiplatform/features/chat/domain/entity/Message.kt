package com.security.chat.multiplatform.features.chat.domain.entity

public sealed interface Message {

    public val id: String
    public val author: MessageAuthor
    public val timestamp: Long
    public val direction: MessageDirection

    public data class Text(
        override val id: String,
        override val author: MessageAuthor,
        override val timestamp: Long,
        override val direction: MessageDirection,
        val text: String,
    ) : Message

    public data class Image(
        override val id: String,
        override val author: MessageAuthor,
        override val timestamp: Long,
        override val direction: MessageDirection,
        val filePath: String?,
    ) : Message
}
