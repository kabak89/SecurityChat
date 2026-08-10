package com.security.chat.multiplatform.features.main.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.features.chat.component.ChatComponentImpl
import com.security.chat.multiplatform.features.chat.component.api.ChatComponent
import com.security.chat.multiplatform.features.chats.component.ChatsComponentImpl
import com.security.chat.multiplatform.features.chats.component.api.ChatsComponent
import com.security.chat.multiplatform.features.main.component.MainComponent.Child.Chat
import com.security.chat.multiplatform.features.main.component.MainComponent.Child.Chats
import kotlinx.serialization.Serializable

public interface MainComponent : BackHandlerOwner {

    public fun onBackClicked()

    public fun openPrivateChat(chatId: String)
    public fun openGroupChat(chatId: String)
    public fun handleSendText(text: String)

    public val childStack: Value<ChildStack<*, Child>>

    public sealed interface Child {
        public class Chats(public val component: ChatsComponent) : Child
        public class Chat(public val component: ChatComponent) : Child
    }

    public sealed interface Params {
        public data class PrivateChat(val chatId: String) : Params
        public data class GroupChat(val chatId: String) : Params
        public data class ShareText(val text: String) : Params
    }
}

public class MainComponentImpl(
    params: MainComponent.Params? = null,
    componentContext: ComponentContext,
) : MainComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Params>()

    private var pendingSharedText: String? = null

    override val childStack: Value<ChildStack<*, MainComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Params.serializer(),
            initialStack = {
                if (params != null) {
                    when (params) {
                        is MainComponent.Params.GroupChat -> {
                            listOf(
                                Params.ChatsParams,
                                Params.GroupChatParams(chatId = params.chatId),
                            )
                        }

                        is MainComponent.Params.PrivateChat -> {
                            listOf(
                                Params.ChatsParams,
                                Params.PrivateChatParams(chatId = params.chatId),
                            )
                        }

                        is MainComponent.Params.ShareText -> {
                            pendingSharedText = params.text
                            listOf(Params.ChatsParams)
                        }
                    }
                } else {
                    listOf(Params.ChatsParams)
                }
            },
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override fun onBackClicked() {
        navigation.pop()
    }

    override fun handleSendText(text: String) {
        pendingSharedText = text
        navigation.replaceAll(Params.ChatsParams)
    }

    override fun openPrivateChat(chatId: String) {
        val top = childStack.value.active.instance

        when (top) {
            is Chat -> {
                val params = top.component.params

                when (params) {
                    is ChatComponent.Params.PersonalChat -> {
                        if (params.value == chatId) {
                            //do nothing
                        } else {
                            navigation.pop()
                            navigation.push(Params.PrivateChatParams(chatId = chatId))
                        }
                    }

                    is ChatComponent.Params.GroupChatId -> {
                        navigation.pop()
                        navigation.push(Params.PrivateChatParams(chatId = chatId))
                    }
                }
            }

            is Chats -> navigation.push(Params.PrivateChatParams(chatId = chatId))
        }
    }

    override fun openGroupChat(chatId: String) {
        val top = childStack.value.active.instance

        when (top) {
            is Chat -> {
                val params = top.component.params

                when (params) {
                    is ChatComponent.Params.GroupChatId -> {
                        if (params.value == chatId) {
                            //do nothing
                        } else {
                            navigation.pop()
                            navigation.push(Params.GroupChatParams(chatId = chatId))
                        }
                    }

                    is ChatComponent.Params.PersonalChat -> {
                        navigation.pop()
                        navigation.push(Params.GroupChatParams(chatId = chatId))
                    }
                }
            }

            is Chats -> navigation.push(Params.GroupChatParams(chatId = chatId))
        }
    }

    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): MainComponent.Child {
        return when (params) {
            is Params.ChatsParams -> {
                Chats(
                    component = ChatsComponentImpl(
                        componentContext = componentContext,
                        onPublicChatClicked = { chatId ->
                            val configuration = Params.PrivateChatParams(
                                chatId = chatId,
                                initialText = pendingSharedText,
                            )
                            pendingSharedText = null
                            navigation.push(configuration = configuration)
                        },
                        onGroupChatClicked = { chatId ->
                            val configuration = Params.GroupChatParams(
                                chatId = chatId,
                                initialText = pendingSharedText,
                            )
                            pendingSharedText = null
                            navigation.push(configuration = configuration)
                        },
                    ),
                )
            }

            is Params.PrivateChatParams -> {
                Chat(
                    component = ChatComponentImpl(
                        componentContext = componentContext,
                        onExit = navigation::pop,
                        params = ChatComponent.Params.PersonalChat(
                            value = params.chatId,
                            initialText = params.initialText,
                        ),
                    ),
                )
            }

            is Params.GroupChatParams -> {
                Chat(
                    component = ChatComponentImpl(
                        componentContext = componentContext,
                        onExit = navigation::pop,
                        params = ChatComponent.Params.GroupChatId(
                            value = params.chatId,
                            initialText = params.initialText,
                        ),
                    ),
                )
            }
        }
    }

    @Serializable
    private sealed interface Params {

        @Serializable
        data object ChatsParams : Params

        @Serializable
        data class PrivateChatParams(
            val chatId: String,
            val initialText: String? = null,
        ) : Params

        @Serializable
        data class GroupChatParams(
            val chatId: String,
            val initialText: String? = null,
        ) : Params
    }
}
