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
import com.security.chat.multiplatform.features.chat.domain.di.chatDomainModule
import com.security.chat.multiplatform.features.chat.ui.di.chatUiModule
import kotlinx.serialization.Serializable

public class ChatComponentImpl(
    override val params: ChatComponent.Params,
    private val onExit: () -> Unit,
    private val onMore: (chatId: String) -> Unit,
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
            initialConfiguration = run {
                when (params) {
                    is ChatComponent.Params.GroupChatId -> {
                        Params.GroupChatParams(
                            chatId = params.value,
                            initialText = params.initialText,
                        )
                    }
                }
            },
            handleBackButton = true,
            childFactory = ::createChild,
        )

    init {
        val featureModules = listOf(
            chatUiModule,
            chatDomainModule,
            chatDataModule,
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
            is Params.GroupChatParams -> {
                ChatComponent.Child.GroupChat(
                    component = GroupChatComponentImpl(
                        componentContext = componentContext,
                        onExit = onExit,
                        chatId = params.chatId,
                        initialText = params.initialText,
                        onMore = {
                            onMore(params.chatId)
                        },
                    ),
                )
            }
        }
    }

    @Serializable
    private sealed class Params {

        @Serializable
        data class GroupChatParams(
            val chatId: String,
            val initialText: String? = null,
        ) : Params()
    }
}

public const val SCOPE_ID_CHAT: String = "SCOPE_ID_CHAT"