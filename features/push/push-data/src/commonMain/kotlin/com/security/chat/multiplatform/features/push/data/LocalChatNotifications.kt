package com.security.chat.multiplatform.features.push.data

import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chats.data.common.ChatsDataHelper
import com.security.chat.multiplatform.features.push.data.storage.PushStorage
import com.security.chat.multiplatform.features.push.domain.entity.NotificationInfo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.storage.UsersStorage
import org.jetbrains.compose.resources.getString
import org.koin.core.module.Module
import securitychat.common.localization.generated.resources.push_channel_name_new_message
import securitychat.common.localization.generated.resources.push_stub_image

public class LocalChatNotifications internal constructor(
    private val chats: ChatsDataHelper,
    private val users: UsersStorage,
    private val userStorage: UserStorage,
    private val chatStorage: ChatStorage,
    private val pushStorage: PushStorage,
    private val presenter: LocalChatNotificationPresenter,
) {

    public suspend fun notifyMeshMessage(chatId: String) {
        if (!pushStorage.isNotificationForChatMustBeShown(chatId)) return
        val user = userStorage.getUserId() ?: return
        val chat = chats.getChatInfo(chatId) ?: return
        val title = (listOf(chat.authorId) + chat.participantIds)
            .distinct().filterNot { it == user }.mapNotNull { users.getUser(it)?.name }
            .joinToString(", ").ifBlank { getString(StringRes.push_channel_name_new_message) }
        val unread = chatStorage.getUnreadMessages(chatId = chatId, userId = user)
        if (unread.isEmpty()) return
        val messages = unread.map {
            when (it) {
                is MessageSM.Text -> it.text
                is MessageSM.Image -> getString(StringRes.push_stub_image)
            }
        }
        presenter.show(chatId, NotificationInfo(title = title, messages = messages))
    }
}

internal expect class LocalChatNotificationPresenter {
    suspend fun show(chatId: String, info: NotificationInfo)
}

internal expect fun Module.registerLocalChatNotificationPresenter()
