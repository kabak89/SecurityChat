package com.security.chat.multiplatform.features.push.domain.entity

public data class NotificationInfo(
    public val title: String,
    public val description: String,
    val chatType: ChatType,
) {
    public enum class ChatType {
        Personal,
        Group,
    }
}