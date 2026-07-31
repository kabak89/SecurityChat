package com.security.chat.multiplatform.features.chat.data.mapper

import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.MessageAuthor
import com.security.chat.multiplatform.features.chat.domain.entity.MessageDirection
import com.security.chat.multiplatform.features.users.data.network.entity.UserNM
import com.security.chat.multiplatform.features.users.data.storage.entity.UserSM

internal fun MessageSM.toDomain(
    appOwnerId: String,
    author: MessageAuthor,
    imagesDirectoryPath: String,
): Message {
    val direction = if (authorId == appOwnerId) {
        MessageDirection.Outgoing
    } else {
        MessageDirection.Incoming
    }

    return when (this) {
        is MessageSM.Text -> Message.Text(
            id = id,
            author = author,
            timestamp = timestamp,
            direction = direction,
            text = text,
        )

        is MessageSM.Image -> Message.Image(
            id = id,
            author = author,
            timestamp = timestamp,
            direction = direction,
            filePath = if (isDownloaded) "$imagesDirectoryPath/$fileId" else null,
        )
    }
}

internal fun UserNM.toSM(): UserSM {
    return UserSM(
        id = userId,
        publicKey = publicKey,
        name = name,
    )
}
