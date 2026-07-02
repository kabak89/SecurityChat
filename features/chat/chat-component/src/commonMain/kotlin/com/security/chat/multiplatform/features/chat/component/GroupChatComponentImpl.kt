package com.security.chat.multiplatform.features.chat.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chat.component.api.GroupChatComponent
import com.security.chat.multiplatform.features.chat.domain.GroupChatModel
import org.koin.core.qualifier.named

public class GroupChatComponentImpl(
    override val chatId: String,
    private val onExit: () -> Unit,
    componentContext: ComponentContext,
) : GroupChatComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_GROUP_CHAT,
    ) {

    init {
        doOnCreate {
            val groupChatModel: GroupChatModel = getKoin().get()
            groupChatModel.start(parentScope = getKoin().get(named(SCOPE_ID_GROUP_CHAT)))
        }
    }

    override fun onExitClicked() {
        onExit()
    }
}

private const val SCOPE_ID_GROUP_CHAT: String = "SCOPE_ID_GROUP_CHAT"