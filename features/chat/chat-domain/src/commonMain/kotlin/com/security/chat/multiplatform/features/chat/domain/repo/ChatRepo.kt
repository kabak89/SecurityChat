package com.security.chat.multiplatform.features.chat.domain.repo

import androidx.paging.PagingData
import com.security.chat.multiplatform.features.chat.domain.entity.ChatInfo
import com.security.chat.multiplatform.features.chat.domain.entity.FileDescriptor
import com.security.chat.multiplatform.features.chat.domain.entity.ImageMessageDescriptor
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.PickedImage
import kotlinx.coroutines.flow.Flow

public interface ChatRepo {

    public suspend fun saveTextMessage(
        message: String,
        chatId: String,
    )

    public suspend fun saveImageMessage(
        chatId: String,
        message: ImageMessageDescriptor,
    )

    public suspend fun uploadMessages(chatId: String)

    public suspend fun fetchMessages(
        chatId: String,
    )

    public fun getMessagesPager(chatId: String): Flow<PagingData<Message>>

    public suspend fun subscribeToNewMessages(chatId: String)
    public suspend fun setUserOnline()

    public suspend fun copyImageToCache(image: PickedImage): FileDescriptor

    public suspend fun createEncryptedFile(
        file: FileDescriptor,
        chatId: String,
    ): ImageMessageDescriptor

    public fun getChatInfoFlow(chatId: String): Flow<ChatInfo?>
}