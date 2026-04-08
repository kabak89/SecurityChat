package com.security.chat.multiplatform.features.chats.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chats.component.api.ChatsComponent
import com.security.chat.multiplatform.features.chats.data.di.chatsDataModule
import com.security.chat.multiplatform.features.chats.data.storage.di.chatsDataStorageModule
import com.security.chat.multiplatform.features.chats.domain.di.chatsDomainModule
import com.security.chat.multiplatform.features.chats.ui.di.chatsUiModule
import kotlinx.serialization.Serializable

public class ChatsComponentImpl(
    private val onChatClicked: (chatId: String) -> Unit,
    private val onSettingsClicked: () -> Unit,
    componentContext: ComponentContext,
) : ChatsComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_CHATS,
    ) {

    private val navigation = StackNavigation<Params>()

    init {
        val featureModules = listOf(
            chatsUiModule,
            chatsDomainModule,
            chatsDataModule,
            chatsDataStorageModule,
        )
        doOnCreate {
            getKoin().loadModules(featureModules)
        }
        doOnDestroy {
            getKoin().unloadModules(featureModules)
        }
    }

    override val childStack: Value<ChildStack<*, ChatsComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Params.serializer(),
            initialConfiguration = Params.ChatListParams,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override fun onBackClicked() {
        navigation.pop()
    }

    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): ChatsComponent.Child {
        return when (params) {
            is Params.ChatListParams -> {
                ChatsComponent.Child.ChatList(
                    component = ChatListComponentImpl(
                        componentContext = componentContext,
                        onAdd = { navigation.push(Params.AddChatParams) },
                        onChatClick = { onChatClicked.invoke(it) },
                        onSettingsClick = onSettingsClicked,
                    ),
                )
            }

            is Params.AddChatParams -> {
                ChatsComponent.Child.AddChat(
                    component = AddChatComponentImpl(
                        componentContext = componentContext,
                        onBack = {
                            navigation.pop()
                        },
                        onChatCreate = { chatId ->
                            navigation.pop()
                            //TODO got to chat
                        },
                    ),
                )
            }
        }
    }

    @Serializable
    private sealed class Params {

        @Serializable
        data object ChatListParams : Params()

        @Serializable
        data object AddChatParams : Params()

    }

}

public const val SCOPE_ID_CHATS: String = "SCOPE_ID_CHATS"