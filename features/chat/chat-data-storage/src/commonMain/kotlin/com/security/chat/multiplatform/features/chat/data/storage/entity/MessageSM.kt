package com.security.chat.multiplatform.features.chat.data.storage.entity

public sealed interface MessageSM {

    public val id: String
    public val chatId: String
    public val recipients: List<MessageRecipientSM>
    public val authorId: String
    public val timestamp: Long
    public val replyToMessageId: String?

    public data class Text(
        override val id: String,
        override val chatId: String,
        override val recipients: List<MessageRecipientSM>,
        override val authorId: String,
        override val timestamp: Long,
        val text: String,
        override val replyToMessageId: String? = null,
    ) : MessageSM

    public data class Image(
        override val id: String,
        override val chatId: String,
        override val recipients: List<MessageRecipientSM>,
        override val authorId: String,
        override val timestamp: Long,
        val fileId: String,
        val key: String,
        val isDownloaded: Boolean,
        override val replyToMessageId: String? = null,
    ) : MessageSM
}

public enum class Status {
    Created,
    Sent,
    ReceivedByReceiver,
    Read,
}
