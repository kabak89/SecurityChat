package com.security.chat.multiplatform.features.chat_info.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chat_info.component.api.ChatInfoMainComponent

public class ChatInfoMainComponentImpl(
    override val chatId: String,
    private val onBack: () -> Unit,
    private val onAddMembers: () -> Unit,
    componentContext: ComponentContext,
) : ChatInfoMainComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_CHAT_INFO_MAIN,
    ) {

    override fun onBackClicked() {
        onBack()
    }

    override fun onAddMembersClicked() {
        onAddMembers()
    }
}

private const val SCOPE_ID_CHAT_INFO_MAIN: String = "SCOPE_ID_CHAT_INFO_MAIN"
