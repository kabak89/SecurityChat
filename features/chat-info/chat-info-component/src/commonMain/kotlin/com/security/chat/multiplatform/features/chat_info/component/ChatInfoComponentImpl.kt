package com.security.chat.multiplatform.features.chat_info.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chat_info.component.api.ChatInfoComponent
import com.security.chat.multiplatform.features.chat_info.ui.di.chatInfoUiModule
import kotlinx.serialization.Serializable

public class ChatInfoComponentImpl(
    override val chatId: String,
    private val onBack: () -> Unit,
    componentContext: ComponentContext,
) : ChatInfoComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_CHAT_INFO,
    ) {

    private val navigation = StackNavigation<Params>()

    override val childStack: Value<ChildStack<*, ChatInfoComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Params.serializer(),
            initialConfiguration = Params.ChatInfoMainParams,
            handleBackButton = true,
            childFactory = ::createChild,
        )

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
        if (childStack.value.items.size > 1) {
            navigation.pop()
        } else {
            onBack()
        }
    }

    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): ChatInfoComponent.Child {
        return when (params) {
            Params.ChatInfoMainParams -> ChatInfoComponent.Child.ChatInfoMain(
                component = ChatInfoMainComponentImpl(
                    chatId = chatId,
                    onBack = ::onBackClicked,
                    onAddMembers = {
                        navigation.pushNew(Params.AddMemberParams)
                    },
                    componentContext = componentContext,
                ),
            )

            Params.AddMemberParams -> ChatInfoComponent.Child.AddMember(
                component = AddMemberComponentImpl(
                    chatId = chatId,
                    onBack = ::onBackClicked,
                    componentContext = componentContext,
                ),
            )
        }
    }

    @Serializable
    private sealed interface Params {
        @Serializable
        data object ChatInfoMainParams : Params

        @Serializable
        data object AddMemberParams : Params
    }
}

private const val SCOPE_ID_CHAT_INFO: String = "SCOPE_ID_CHAT_INFO"
