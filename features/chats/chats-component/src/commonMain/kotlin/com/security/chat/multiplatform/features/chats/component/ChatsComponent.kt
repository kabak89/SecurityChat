package com.security.chat.multiplatform.features.chats.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.common.core.component.DiScopeHolder
import kotlinx.serialization.Serializable

public interface ChatsComponent : BaseComponent, DiScopeHolder, BackHandlerOwner {

    public fun onBackClicked()

    public val childStack: Value<ChildStack<*, Child>>

    public sealed interface Child {

        public class ChatList(public val component: ChatListComponent) : Child
        public class AddChat(public val component: AddChatComponent) : Child

    }

}

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