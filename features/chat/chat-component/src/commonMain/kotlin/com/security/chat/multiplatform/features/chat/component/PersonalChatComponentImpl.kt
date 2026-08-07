package com.security.chat.multiplatform.features.chat.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chat.component.api.PersonalChatComponent
import com.security.chat.multiplatform.features.chat.domain.PersonalChatModel

public class PersonalChatComponentImpl(
    override val chatId: String,
    private val initialText: String? = null,
    private val onExit: () -> Unit,
    componentContext: ComponentContext,
) : PersonalChatComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_PERSONAL_CHAT,
    ) {

    init {
        doOnCreate {
            val personalChatModel: PersonalChatModel = getKoin().get()
            personalChatModel.start(parentScope = componentCoroutineScope)
            if (initialText != null) {
                personalChatModel.setCurrentMessageText(initialText)
            }
        }
    }

    override fun onExitClicked() {
        onExit()
    }
}

private const val SCOPE_ID_PERSONAL_CHAT: String = "SCOPE_ID_PERSONAL_CHAT"