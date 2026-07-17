package com.security.chat.multiplatform.features.chats.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chats.component.api.AddChatComponent
import com.security.chat.multiplatform.features.chats.domain.CreateChatModel

public class AddChatComponentImpl(
    private val onBack: () -> Unit,
    private val onPersonalChatCreate: (chatId: String) -> Unit,
    private val onGroupChatCreate: (chatId: String) -> Unit,
    componentContext: ComponentContext,
) : AddChatComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_ADD_CHAT,
    ) {

    init {
        val createChatModel: CreateChatModel = getKoin().get()
        createChatModel.start(parentScope = componentCoroutineScope)
    }

    override fun onBackClicked() {
        onBack()
    }

    override fun onPersonalChatCreated(chatId: String) {
        onPersonalChatCreate(chatId)
    }

    override fun onGroupChatCreated(chatId: String) {
        onGroupChatCreate(chatId)
    }
}

public const val SCOPE_ID_ADD_CHAT: String = "SCOPE_ID_ADD_CHAT"