package com.security.chat.multiplatform.features.chat.ui.screens.personalchat.mapper

import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.entity.MessageUM

internal fun Message.toUi(): MessageUM {
    return MessageUM(
        id = id,
        text = text,
    )
}