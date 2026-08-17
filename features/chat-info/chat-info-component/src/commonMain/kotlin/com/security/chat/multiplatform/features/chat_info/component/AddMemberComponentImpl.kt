package com.security.chat.multiplatform.features.chat_info.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chat_info.component.api.AddMemberComponent
import com.security.chat.multiplatform.features.chat_info.domain.AddMemberModel
import com.security.chat.multiplatform.features.chat_info.domain.di.chatInfoDomainAddMemberModule

public class AddMemberComponentImpl(
    override val chatId: String,
    private val onBack: () -> Unit,
    componentContext: ComponentContext,
) : AddMemberComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_ADD_MEMBER,
    ) {

    init {
        val featureModules = listOf(
            chatInfoDomainAddMemberModule,
        )
        getKoin().loadModules(featureModules)

        doOnCreate {
            val addMemberModel: AddMemberModel = getKoin().get()
            addMemberModel.start(parentScope = componentCoroutineScope)
        }

        doOnDestroy {
            getKoin().unloadModules(featureModules)
        }
    }

    override fun onBackClicked() {
        onBack()
    }

    override fun onMembersAdded() {
        onBack()
    }
}

private const val SCOPE_ID_ADD_MEMBER: String = "SCOPE_ID_ADD_MEMBER"
