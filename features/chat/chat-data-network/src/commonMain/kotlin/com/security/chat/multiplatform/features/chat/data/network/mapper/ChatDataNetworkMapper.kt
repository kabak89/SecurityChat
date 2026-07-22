package com.security.chat.multiplatform.features.chat.data.network.mapper

import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatMessageNM
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ChatMessage
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ImageMessageResponse
import com.security.chat.multiplatform.features.chat.data.network.entity.network.TextMessageResponse
import kotlinx.serialization.json.Json

internal fun ChatMessage.toNM(
    json: Json,
): ChatMessageNM? {
    return when (type) {
        "text" -> {
            val textMessageResponse: TextMessageResponse = json.decodeFromString(message)
            textMessageResponse.toNM()
        }

        "image" -> {
            val imageMessageResponse: ImageMessageResponse = json.decodeFromString(message)
            imageMessageResponse.toNM()
        }

        else -> {
            Log.e("unknown type $type")
            null
        }
    }
}

private fun ImageMessageResponse.toNM(): ChatMessageNM.Image {
    return ChatMessageNM.Image(
        id = this.id,
        key = this.key,
        authorId = this.authorId,
        timestamp = this.timestamp,
        fileId = this.fileId,
    )
}

private fun TextMessageResponse.toNM(): ChatMessageNM.Text {
    return ChatMessageNM.Text(
        id = this.id,
        key = this.key,
        authorId = this.authorId,
        timestamp = this.timestamp,
        text = this.text,
    )
}