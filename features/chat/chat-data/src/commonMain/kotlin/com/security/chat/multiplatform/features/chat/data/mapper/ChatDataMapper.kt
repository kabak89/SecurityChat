package com.security.chat.multiplatform.features.chat.data.mapper

import com.security.chat.multiplatform.features.chat.data.network.entity.ChatMessageNM
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.Status
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.MessageAuthor
import com.security.chat.multiplatform.features.chat.domain.entity.MessageDirection
import com.security.chat.multiplatform.features.users.data.network.entity.UserNM
import com.security.chat.multiplatform.features.users.data.storage.entity.UserSM

internal suspend fun ChatMessageNM.toDomain(
    decryptMessage: suspend (encryptedText: String) -> String,
    appOwnerId: String,
    author: MessageAuthor,
): Message {
    val direction = if (authorId == appOwnerId) {
        MessageDirection.Outgoing
    } else {
        MessageDirection.Incoming
    }

    return Message(
        id = id,
        text = decryptMessage(text),
        author = author,
        timestamp = timestamp,
        direction = direction,
    )
}

internal fun MessageSM.toDomain(
    appOwnerId: String,
    author: MessageAuthor,
): Message {
    val direction = if (authorId == appOwnerId) {
        MessageDirection.Outgoing
    } else {
        MessageDirection.Incoming
    }

    val text = when (this) {
        is MessageSM.Text -> text
    }

    return Message(
        id = id,
        text = text,
        author = author,
        timestamp = timestamp,
        direction = direction,
    )
}

internal fun Message.toSM(
    chatId: String,
    recipients: List<String>,
): MessageSM {
    return MessageSM.Text(
        id = id,
        text = text,
        authorId = author.id,
        chatId = chatId,
        //TODO
        status = Status.Received,
        timestamp = timestamp,
        recipients = recipients,
    )
}

internal fun UserNM.toSM(): UserSM {
    return UserSM(
        id = userId,
        publicKey = publicKey,
        name = name,
    )
}