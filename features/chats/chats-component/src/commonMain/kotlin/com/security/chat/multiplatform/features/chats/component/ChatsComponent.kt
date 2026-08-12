package com.security.chat.multiplatform.features.chats.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.chats.component.api.ChatsComponent
import com.security.chat.multiplatform.features.chats.component.api.ChatsComponent.Child.ChatList
import com.security.chat.multiplatform.features.chats.data.di.chatsDataModule
import com.security.chat.multiplatform.features.chats.domain.di.chatsDomainModule
import com.security.chat.multiplatform.features.chats.ui.di.chatsUiModule
import com.security.chat.multiplatform.features.users.data.network.di.usersNetworkManager
import kotlinx.serialization.Serializable

public class ChatsComponentImpl(
    private val onGroupChatClicked: (chatId: String) -> Unit,
    private val onSettingsClicked: () -> Unit,
    private val onAddChatClicked: () -> Unit,
    componentContext: ComponentContext,
) : ChatsComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_CHATS,
    ) {

    private val navigation = StackNavigation<Params>()

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

    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): ChatsComponent.Child {
        return when (params) {
            is Params.ChatListParams -> {
                ChatList(
                    component = ChatListComponentImpl(
                        componentContext = componentContext,
                        onAdd = onAddChatClicked,
                        onGroupChatClick = onGroupChatClicked,
                        onSettingsClick = onSettingsClicked,
                    ),
                )
            }
        }
    }

    @Serializable
    private sealed class Params {

        @Serializable
        data object ChatListParams : Params()
    }
}

public const val SCOPE_ID_CHATS: String = "SCOPE_ID_CHATS"