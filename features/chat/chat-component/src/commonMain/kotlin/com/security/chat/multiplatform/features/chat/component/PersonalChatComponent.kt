package com.security.chat.multiplatform.features.chat.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface PersonalChatComponent : BaseComponent, DiScopeHolder {
    public fun onBackClicked()
}

public class PersonalChatComponentImpl(
    private val onBack: () -> Unit,
    componentContext: ComponentContext,
) : PersonalChatComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_PERSONAL_CHAT,
    ) {

    override fun onBackClicked() {
        onBack()
    }

}

public const val SCOPE_ID_PERSONAL_CHAT: String = "SCOPE_ID_PERSONAL_CHAT"