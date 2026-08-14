package com.security.chat.multiplatform.features.chat.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chat.component.api.GroupChatComponent
import com.security.chat.multiplatform.features.chat.domain.GroupChatModel

public class GroupChatComponentImpl(
    override val chatId: String,
    private val initialText: String? = null,
    private val onExit: () -> Unit,
    private val onMore: () -> Unit,
    componentContext: ComponentContext,
) : GroupChatComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_GROUP_CHAT,
    ) {

    init {
        doOnCreate {
            val groupChatModel: GroupChatModel = getKoin().get()
            groupChatModel.start(parentScope = componentCoroutineScope)
            if (initialText != null) {
                groupChatModel.setCurrentMessageText(initialText)
            }
        }
    }

    override fun onExitClicked() {
        onExit()
    }

    override fun onMoreClicked() {
        onMore()
    }
}

private const val SCOPE_ID_GROUP_CHAT: String = "SCOPE_ID_GROUP_CHAT"