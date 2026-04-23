package com.security.chat.multiplatform.features.chat.ui.screens.personalchat.mapper

import com.security.chat.multiplatform.features.chat.domain.entity.Interlocutor
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.MessageDirection
import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.entity.InterlocutorUM
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

internal fun Interlocutor.toUi(): InterlocutorUM {
    val nameText = if (isOnline) {
        "$name ●"
    } else {
        name
    }

    return InterlocutorUM(
        name = nameText,
        isOnline = isOnline,
    )
}