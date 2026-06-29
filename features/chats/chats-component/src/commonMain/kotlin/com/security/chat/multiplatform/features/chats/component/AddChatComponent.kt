package com.security.chat.multiplatform.features.chats.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chats.component.api.AddChatComponent
import com.security.chat.multiplatform.features.chats.domain.CreateChatModel
import org.koin.core.qualifier.named

public class AddChatComponentImpl(
    private val onBack: () -> Unit,
    private val onChatCreate: (chatId: String) -> Unit,
    componentContext: ComponentContext,
) : AddChatComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_ADD_CHAT,
    ) {

    init {
        val createChatModel: CreateChatModel = getKoin().get()
        createChatModel.start(parentScope = getKoin().get(named(SCOPE_ID_CHATS)))
    }

    override fun onBackClicked() {
        onBack()
    }

    override fun onChatCreated(chatId: String) {
        onChatCreate(chatId)
    }

}

public const val SCOPE_ID_ADD_CHAT: String = "SCOPE_ID_ADD_CHAT"