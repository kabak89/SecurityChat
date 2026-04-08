package com.security.chat.multiplatform.features.chats.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chats.component.api.AddChatComponent

public class AddChatComponentImpl(
    private val onBack: () -> Unit,
    private val onChatCreate: (chatId: String) -> Unit,
    componentContext: ComponentContext,
) : AddChatComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_ADD_CHAT,
    ) {

    override fun onBackClicked() {
        onBack()
    }

    override fun onChatCreated(chatId: String) {
        onChatCreate(chatId)
    }

}

public const val SCOPE_ID_ADD_CHAT: String = "SCOPE_ID_ADD_CHAT"