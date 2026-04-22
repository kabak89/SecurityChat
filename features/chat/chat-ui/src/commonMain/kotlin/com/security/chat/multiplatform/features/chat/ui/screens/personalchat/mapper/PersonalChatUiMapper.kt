package com.security.chat.multiplatform.features.chat.ui.screens.personalchat.mapper

import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.MessageDirection
import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.entity.MessageUM

internal fun Message.toUi(): MessageUM {
    return when (this.direction) {
        MessageDirection.Incoming -> {
            MessageUM.Incoming(
                id = id,
                text = text,
            )
        }

        MessageDirection.Outgoing -> {
            MessageUM.Outgoing(
                id = id,
                text = text,
            )
        }
    }
}