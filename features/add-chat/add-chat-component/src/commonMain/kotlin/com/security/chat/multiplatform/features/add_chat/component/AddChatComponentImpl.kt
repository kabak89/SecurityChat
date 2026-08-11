package com.security.chat.multiplatform.features.add_chat.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.add_chat.component.api.AddChatComponent
import com.security.chat.multiplatform.features.add_chat.data.di.addChatDataModule
import com.security.chat.multiplatform.features.add_chat.domain.CreateChatModel
import com.security.chat.multiplatform.features.add_chat.domain.di.addChatDomainModule
import com.security.chat.multiplatform.features.add_chat.ui.di.addChatUiModule

public class AddChatComponentImpl(
    private val onBack: () -> Unit,
    private val onGroupChatCreate: (chatId: String) -> Unit,
    componentContext: ComponentContext,
) : AddChatComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_ADD_CHAT,
    ) {

    init {
        val featureModules = listOf(
            addChatDomainModule,
            addChatDataModule,
            addChatUiModule,
        )
        getKoin().loadModules(featureModules)
        doOnDestroy {
            getKoin().unloadModules(featureModules)
        }

        val createChatModel: CreateChatModel = getKoin().get()
        createChatModel.start(parentScope = componentCoroutineScope)
    }

    override fun onBackClicked() {
        onBack()
    }

    override fun onGroupChatCreated(chatId: String) {
        onGroupChatCreate(chatId)
    }
}

public const val SCOPE_ID_ADD_CHAT: String = "SCOPE_ID_ADD_CHAT"
