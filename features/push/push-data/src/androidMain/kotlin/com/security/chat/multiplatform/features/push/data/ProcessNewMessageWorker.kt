package com.security.chat.multiplatform.features.push.data

import com.security.chat.multiplatform.common.background.work.BackgroundWorker
import com.security.chat.multiplatform.features.push.data.storage.PushStorage
import com.security.chat.multiplatform.features.push.domain.PushRepository

internal class ProcessNewMessageWorker(
    private val pushRepository: PushRepository,
    private val pushStorage: PushStorage,
    private val presenter: LocalChatNotificationPresenter,
) : BackgroundWorker {

    override suspend fun run(input: Map<String, String>) {
        val messageType = checkNotNull(input[KEY_TYPE]) { "No push message type" }
        when (messageType) {
            TYPE_CHAT_MESSAGE -> handleChatMessage(input)
            else -> error("Unknown push message type: $messageType")
        }
    }

    private suspend fun handleChatMessage(input: Map<String, String>) {
        val chatId = checkNotNull(input[KEY_CHAT_ID]) { "No chatId in push message" }
        if (!pushStorage.isNotificationForChatMustBeShown(chatId)) return
        val serializedMessages = checkNotNull(input[KEY_MESSAGES]) { "No messages in push message" }
        val notificationInfo = pushRepository.processNewMessages(
            serializedMessages = serializedMessages,
            chatId = chatId,
        )
        if (!pushStorage.isNotificationForChatMustBeShown(chatId)) {
            pushRepository.clearNotificationsForChat(chatId)
            return
        }
        if (notificationInfo.messages.isEmpty()) return
        presenter.show(chatId, notificationInfo)
    }

    internal companion object {
        const val KEY_TYPE: String = "type"
        const val KEY_CHAT_ID: String = "chatId"
        const val KEY_MESSAGES: String = "messages"
    }
}

private const val TYPE_CHAT_MESSAGE = "chat_message"
