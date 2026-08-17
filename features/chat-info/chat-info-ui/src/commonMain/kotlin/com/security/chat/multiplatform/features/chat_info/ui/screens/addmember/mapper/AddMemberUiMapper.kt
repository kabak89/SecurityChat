package com.security.chat.multiplatform.features.chat_info.ui.screens.addmember.mapper

import com.security.chat.multiplatform.features.chat_info.domain.entity.ChatMember
import com.security.chat.multiplatform.features.chat_info.ui.screens.addmember.entity.FoundMember

internal fun ChatMember.toUi(): FoundMember {
    return FoundMember(
        id = id,
        name = username,
    )
}