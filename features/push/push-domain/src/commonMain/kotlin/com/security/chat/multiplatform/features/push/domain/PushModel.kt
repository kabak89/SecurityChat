package com.security.chat.multiplatform.features.push.domain

public interface PushModel {
    public suspend fun registerCurrentToken()
    public fun setShowNotificationsForChat(chatId: String, show: Boolean)
    public fun clearNotificationsForChat(chatId: String)
}

internal class PushModelImpl(
    private val pushRepository: PushRepository,
) : PushModel {

    override suspend fun registerCurrentToken() {
        pushRepository.registerCurrentToken()
    }

    override fun setShowNotificationsForChat(chatId: String, show: Boolean) {
        pushRepository.setShowNotificationsForChat(
            chatId = chatId,
            show = show,
        )
    }

    override fun clearNotificationsForChat(chatId: String) {
        pushRepository.clearNotificationsForChat(chatId)
    }
}