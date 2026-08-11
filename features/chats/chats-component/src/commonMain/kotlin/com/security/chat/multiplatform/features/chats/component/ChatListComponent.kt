package com.security.chat.multiplatform.features.chats.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chats.component.api.ChatListComponent
import com.security.chat.multiplatform.features.chats.domain.ChatsModel

public class ChatListComponentImpl(
    private val onAdd: () -> Unit,
    private val onGroupChatClick: (chatId: String) -> Unit,
    private val onSettingsClick: () -> Unit,
    componentContext: ComponentContext,
) : ChatListComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_CHAT_LIST,
    ) {

    init {
        doOnCreate {
            val chatsModel: ChatsModel = getKoin().get()
            chatsModel.start(parentScope = componentCoroutineScope)
        }
    }

    override fun onAddClicked() {
        onAdd()
    }

    override fun onGroupChatClicked(chatId: String) {
        onGroupChatClick(chatId)
    }

    override fun onSettingsClicked() {
        onSettingsClick()
    }
}

public const val SCOPE_ID_CHAT_LIST: String = "SCOPE_ID_CHAT_LIST"