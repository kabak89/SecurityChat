package com.security.chat.multiplatform.features.chat.data.storage.entity

public data class MessageReceivedEventSM(
    val id: String,
    val messageId: String,
    val chatId: String,
    val recipientUserId: String,
    val receivedByUserId: String,
)
