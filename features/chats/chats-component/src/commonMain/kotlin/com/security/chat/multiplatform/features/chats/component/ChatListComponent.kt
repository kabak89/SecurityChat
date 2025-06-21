package com.security.chat.multiplatform.features.chats.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface ChatListComponent : BaseComponent, DiScopeHolder {
    public fun onAddClicked()
}

public class ChatListComponentImpl(
    private val onAdd: () -> Unit,
    componentContext: ComponentContext,
) : ChatListComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_CHAT_LIST,
    ) {

    override fun onAddClicked() {
        onAdd()
    }
}

public const val SCOPE_ID_CHAT_LIST: String = "SCOPE_ID_CHAT_LIST"