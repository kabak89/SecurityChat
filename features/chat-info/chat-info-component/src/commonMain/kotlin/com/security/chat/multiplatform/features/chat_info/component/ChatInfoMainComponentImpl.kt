package com.security.chat.multiplatform.features.chat_info.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chat_info.component.api.ChatInfoMainComponent
import com.security.chat.multiplatform.features.chat_info.data.di.chatInfoDataModule
import com.security.chat.multiplatform.features.chat_info.domain.ChatInfoModel
import com.security.chat.multiplatform.features.chat_info.domain.di.chatInfoDomainInfoModule
import com.security.chat.multiplatform.features.chat_info.ui.di.chatInfoUiModule

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

    init {
        val featureModules = listOf(
            chatInfoDomainInfoModule,
            chatInfoDataModule,
            chatInfoUiModule,
        )
        getKoin().loadModules(featureModules)

        doOnCreate {
            val chatInfoModel: ChatInfoModel = getKoin().get()
            chatInfoModel.start(parentScope = componentCoroutineScope)
        }

        doOnDestroy {
            getKoin().unloadModules(featureModules)
        }
    }

    override fun onBackClicked() {
        onBack()
    }

    override fun onAddMembersClicked() {
        onAddMembers()
    }
}

private const val SCOPE_ID_CHAT_INFO_MAIN: String = "SCOPE_ID_CHAT_INFO_MAIN"
