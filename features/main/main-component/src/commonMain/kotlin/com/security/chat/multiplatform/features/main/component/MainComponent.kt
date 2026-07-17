package com.security.chat.multiplatform.features.main.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.features.chat.component.ChatComponentImpl
import com.security.chat.multiplatform.features.chat.component.api.ChatComponent
import com.security.chat.multiplatform.features.chats.component.ChatsComponentImpl
import com.security.chat.multiplatform.features.chats.component.api.ChatsComponent
import com.security.chat.multiplatform.features.main.component.MainComponent.Child.Chat
import com.security.chat.multiplatform.features.main.component.MainComponent.Child.Chats
import com.security.chat.multiplatform.features.main.component.MainComponent.Child.Settings
import com.security.chat.multiplatform.features.settings.component.SettingsComponentImpl
import com.security.chat.multiplatform.features.settings.component.api.SettingsComponent
import kotlinx.serialization.Serializable

public interface MainComponent : BackHandlerOwner {

    public fun onBackClicked()

    public fun openPrivateChat(chatId: String)
    public fun openGroupChat(chatId: String)

    public val childStack: Value<ChildStack<*, Child>>

    public sealed interface Child {
        public class Chats(public val component: ChatsComponent) : Child
        public class Settings(public val component: SettingsComponent) : Child
        public class Chat(public val component: ChatComponent) : Child
    }

    public sealed interface Params {
        public data class PrivateChat(val chatId: String) : Params
        public data class GroupChat(val chatId: String) : Params
    }
}

public class MainComponentImpl(
    private val onLogout: () -> Unit,
    params: MainComponent.Params? = null,
    componentContext: ComponentContext,
) : MainComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Params>()

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
            is Settings -> {
                navigation.pop()
                navigation.push(Params.PrivateChatParams(chatId = chatId))
            }
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
            is Settings -> {
                navigation.pop()
                navigation.push(Params.GroupChatParams(chatId = chatId))
            }
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
                            )
                            navigation.push(configuration = configuration)
                        },
                        onSettingsClicked = {
                            navigation.push(configuration = Params.SettingsParams)
                        },
                        onGroupChatClicked = { chatId ->
                            val configuration = Params.GroupChatParams(
                                chatId = chatId,
                            )
                            navigation.push(configuration = configuration)
                        },
                    ),
                )
            }

            is Params.SettingsParams -> {
                Settings(
                    component = SettingsComponentImpl(
                        componentContext = componentContext,
                        onExit = navigation::pop,
                        onLogout = onLogout,
                    ),
                )
            }

            is Params.PrivateChatParams -> {
                Chat(
                    component = ChatComponentImpl(
                        componentContext = componentContext,
                        onExit = navigation::pop,
                        params = ChatComponent.Params.PersonalChat(params.chatId),
                    ),
                )
            }

            is Params.GroupChatParams -> {
                Chat(
                    component = ChatComponentImpl(
                        componentContext = componentContext,
                        onExit = navigation::pop,
                        params = ChatComponent.Params.GroupChatId(params.chatId),
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
        data object SettingsParams : Params

        @Serializable
        data class PrivateChatParams(
            val chatId: String,
        ) : Params

        @Serializable
        data class GroupChatParams(
            val chatId: String,
        ) : Params
    }
}
