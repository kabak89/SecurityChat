package com.security.chat.multiplatform.features.chat_info.domain.entity

public data class StateInfo(
    val memberNameForSearch: String,
    val foundMembers: List<ChatMember>,
)
