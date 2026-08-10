package com.security.chat.multiplatform.features.chats.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chats.component.api.ChatsComponent
import com.security.chat.multiplatform.features.chats.component.api.ChatsComponent.Child.AddChat
import com.security.chat.multiplatform.features.chats.component.api.ChatsComponent.Child.ChatList
import com.security.chat.multiplatform.features.chats.data.di.chatsDataModule
import com.security.chat.multiplatform.features.chats.domain.di.chatsDomainModule
import com.security.chat.multiplatform.features.chats.ui.di.chatsUiModule
import com.security.chat.multiplatform.features.add_chat.component.AddChatComponentImpl
import com.security.chat.multiplatform.features.settings.component.SettingsComponentImpl
import com.security.chat.multiplatform.features.users.data.network.di.usersNetworkManager
import kotlinx.serialization.Serializable

public class ChatsComponentImpl(
    private val onPublicChatClicked: (chatId: String) -> Unit,
    private val onGroupChatClicked: (chatId: String) -> Unit,
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
            usersNetworkManager,
        )
        getKoin().loadModules(featureModules)
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
                ChatList(
                    component = ChatListComponentImpl(
                        componentContext = componentContext,
                        onAdd = { navigation.push(Params.AddChatParams) },
                        onPersonalChatClick = onPublicChatClicked,
                        onGroupChatClick = onGroupChatClicked,
                        onSettingsClick = {
                            navigation.push(configuration = Params.SettingsParams)
                        },
                    ),
                )
            }

            is Params.AddChatParams -> {
                AddChat(
                    component = AddChatComponentImpl(
                        componentContext = componentContext,
                        onBack = {
                            navigation.pop()
                        },
                        onPersonalChatCreate = { chatId ->
                            navigation.pop()
                            onPublicChatClicked(chatId)
                        },
                        onGroupChatCreate = { chatId ->
                            navigation.pop()
                            onGroupChatClicked(chatId)
                        },
                    ),
                )
            }

            Params.SettingsParams -> {
                ChatsComponent.Child.Settings(
                    component = SettingsComponentImpl(
                        componentContext = componentContext,
                        onExit = navigation::pop,
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

        @Serializable
        data object SettingsParams : Params()
    }
}

public const val SCOPE_ID_CHATS: String = "SCOPE_ID_CHATS"