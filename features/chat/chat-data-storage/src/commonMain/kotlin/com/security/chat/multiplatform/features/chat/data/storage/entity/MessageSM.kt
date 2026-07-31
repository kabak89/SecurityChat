package com.security.chat.multiplatform.features.chat.data.storage.entity

public sealed interface MessageSM {

    public val id: String
    public val chatId: String
    public val recipients: List<String>
    public val authorId: String
    public val status: Status
    public val timestamp: Long

    public data class Text(
        override val id: String,
        override val chatId: String,
        override val recipients: List<String>,
        override val authorId: String,
        override val status: Status,
        override val timestamp: Long,
        val text: String,
    ) : MessageSM

    public data class Image(
        override val id: String,
        override val chatId: String,
        override val recipients: List<String>,
        override val authorId: String,
        override val status: Status,
        override val timestamp: Long,
        val fileId: String,
        val key: String,
        val isDownloaded: Boolean,
    ) : MessageSM
}

public enum class Status {
    Created,
    Sent,
    Received,
}
