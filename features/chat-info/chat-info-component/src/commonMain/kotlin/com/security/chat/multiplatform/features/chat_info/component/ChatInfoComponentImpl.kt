package com.security.chat.multiplatform.features.chat_info.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chat_info.component.api.ChatInfoComponent
import com.security.chat.multiplatform.features.chat_info.ui.di.chatInfoUiModule

public class ChatInfoComponentImpl(
    override val chatId: String,
    private val onBack: () -> Unit,
    componentContext: ComponentContext,
) : ChatInfoComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_CHAT_INFO,
    ) {

    init {
        val featureModules = listOf(
            chatInfoUiModule,
        )
        getKoin().loadModules(featureModules)
        doOnDestroy {
            getKoin().unloadModules(featureModules)
        }
    }

    override fun onBackClicked() {
        onBack()
    }
}

private const val SCOPE_ID_CHAT_INFO: String = "SCOPE_ID_CHAT_INFO"
