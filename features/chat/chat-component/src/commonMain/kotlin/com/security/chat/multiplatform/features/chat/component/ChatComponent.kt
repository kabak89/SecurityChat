package com.security.chat.multiplatform.features.chat.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chat.component.api.ChatComponent
import com.security.chat.multiplatform.features.chat.data.di.chatDataModule
import com.security.chat.multiplatform.features.chat.data.storage.di.chatDataStorageModule
import com.security.chat.multiplatform.features.chat.domain.di.chatDomainModule
import com.security.chat.multiplatform.features.chat.ui.di.chatUiModule
import kotlinx.serialization.Serializable

public class ChatComponentImpl(
    override val chatId: String,
    private val onExit: () -> Unit,
    componentContext: ComponentContext,
) : ChatComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_CHAT,
    ) {

    private val navigation = StackNavigation<Params>()

    override val childStack: Value<ChildStack<*, ChatComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Params.serializer(),
            initialConfiguration = Params.PersonalChatParams(
                chatId = chatId,
            ),
            handleBackButton = true,
            childFactory = ::createChild,
        )

    init {
        val featureModules = listOf(
            chatUiModule,
            chatDomainModule,
            chatDataModule,
            chatDataStorageModule,
        )
        getKoin().loadModules(featureModules)
        doOnDestroy {
            getKoin().unloadModules(featureModules)
        }
    }

    override fun onBackClicked() {
        navigation.pop()
    }

    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): ChatComponent.Child {
        return when (params) {
            is Params.PersonalChatParams -> {
                ChatComponent.Child.PersonalChat(
                    component = PersonalChatComponentImpl(
                        componentContext = componentContext,
                        onExit = onExit,
                        chatId = params.chatId,
                    ),
                )
            }
        }
    }

    @Serializable
    private sealed class Params {

        @Serializable
        data class PersonalChatParams(
            val chatId: String,
        ) : Params()

    }

}

public const val SCOPE_ID_CHAT: String = "SCOPE_ID_CHAT"