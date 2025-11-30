package com.security.chat.multiplatform.features.chat.data.storage.entity

public data class MessageSM(
    val id: String,
    val chatId: String,
    val text: String,
    val authorId: String,
    val status: Status,
    val timestamp: Long,
) {

    public enum class Status {
        Created,
        Sent,
        Received,
    }

}
