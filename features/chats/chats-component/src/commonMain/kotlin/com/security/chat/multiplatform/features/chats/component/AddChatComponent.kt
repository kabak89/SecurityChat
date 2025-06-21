package com.security.chat.multiplatform.features.chats.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface AddChatComponent : BaseComponent, DiScopeHolder {
    public fun onBackClicked()
}

public class AddChatComponentImpl(
    private val onBack: () -> Unit,
    componentContext: ComponentContext,
) : AddChatComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_ADD_CHAT,
    ) {

    override fun onBackClicked() {
        onBack()
    }

}

public const val SCOPE_ID_ADD_CHAT: String = "SCOPE_ID_ADD_CHAT"