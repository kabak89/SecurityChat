package com.security.chat.multiplatform.features.chats.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface ChatListComponent : BaseComponent, DiScopeHolder {
    public fun onAddClicked()
    public fun onChatClicked(chatId: String)
    public fun onSettingsClicked()
}

public class ChatListComponentImpl(
    private val onAdd: () -> Unit,
    private val onChatClick: (chatId: String) -> Unit,
    private val onSettingsClick: () -> Unit,
    componentContext: ComponentContext,
) : ChatListComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_CHAT_LIST,
    ) {

    override fun onAddClicked() {
        onAdd()
    }

    override fun onChatClicked(chatId: String) {
        onChatClick(chatId)
    }

    override fun onSettingsClicked() {
        onSettingsClick()
    }
}

public const val SCOPE_ID_CHAT_LIST: String = "SCOPE_ID_CHAT_LIST"