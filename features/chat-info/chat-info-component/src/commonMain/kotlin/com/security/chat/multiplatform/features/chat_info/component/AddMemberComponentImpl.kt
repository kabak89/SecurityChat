package com.security.chat.multiplatform.features.chat_info.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chat_info.component.api.AddMemberComponent

public class AddMemberComponentImpl(
    override val chatId: String,
    private val onBack: () -> Unit,
    componentContext: ComponentContext,
) : AddMemberComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_ADD_MEMBER,
    ) {

    override fun onBackClicked() {
        onBack()
    }
}

private const val SCOPE_ID_ADD_MEMBER: String = "SCOPE_ID_ADD_MEMBER"
