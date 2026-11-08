package com.security.chat.multiplatform.features.chat.domain.entity

public sealed interface MessageQuote {
    public val messageId: String

    public data class Text(override val messageId: String, val text: String) : MessageQuote
    public data class Image(override val messageId: String, val filePath: String) : MessageQuote
}
