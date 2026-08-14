package com.security.chat.multiplatform.features.main.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.security.chat.multiplatform.features.add_chat.component.AddChatComponentImpl
import com.security.chat.multiplatform.features.chat.component.ChatComponentImpl
import com.security.chat.multiplatform.features.chat.component.api.ChatComponent
import com.security.chat.multiplatform.features.chat_info.component.ChatInfoComponentImpl
import com.security.chat.multiplatform.features.chats.component.ChatsComponentImpl
import com.security.chat.multiplatform.features.main.component.api.MainComponent
import com.security.chat.multiplatform.features.main.component.api.MainComponent.Child.Chat
import com.security.chat.multiplatform.features.main.component.api.MainComponent.Child.Chats
import com.security.chat.multiplatform.features.settings.component.SettingsComponentImpl
import kotlinx.serialization.Serializable

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

    override fun openGroupChat(chatId: String) {
        val configuration = Params.GroupChatParams(
            chatId = chatId,
            initialText = pendingSharedText,
        )
        pendingSharedText = null

        val top = childStack.value.active.instance

        when (top) {
            is Chat -> {
                val params = top.component.params
                if (params is ChatComponent.Params.GroupChatId && params.value == chatId) {
                    // do nothing
                } else {
                    navigation.pop()
                    navigation.push(configuration)
                }
            }

            else -> navigation.push(configuration)
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
                        onGroupChatClicked = ::openGroupChat,
                        onSettingsClicked = {
                            navigation.push(Params.SettingsParams)
                        },
                        onAddChatClicked = {
                            navigation.push(Params.AddChatParams)
                        },
                    ),
                )
            }

            is Params.GroupChatParams -> {
                Chat(
                    component = ChatComponentImpl(
                        componentContext = componentContext,
                        onExit = navigation::pop,
                        onMore = { chatId ->
                            navigation.push(Params.ChatInfoParams(chatId = chatId))
                        },
                        params = ChatComponent.Params.GroupChatId(
                            value = params.chatId,
                            initialText = params.initialText,
                        ),
                    ),
                )
            }

            is Params.ChatInfoParams -> {
                MainComponent.Child.ChatInfo(
                    component = ChatInfoComponentImpl(
                        chatId = params.chatId,
                        onBack = navigation::pop,
                        componentContext = componentContext,
                    ),
                )
            }

            Params.SettingsParams -> {
                MainComponent.Child.Settings(
                    component = SettingsComponentImpl(
                        componentContext = componentContext,
                        onExit = navigation::pop,
                    ),
                )
            }

            Params.AddChatParams -> {
                MainComponent.Child.AddChat(
                    component = AddChatComponentImpl(
                        componentContext = componentContext,
                        onBack = navigation::pop,
                        onGroupChatCreate = { chatId ->
                            navigation.pop()
                            openGroupChat(chatId)
                        },
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
        data object AddChatParams : Params

        @Serializable
        data class GroupChatParams(
            val chatId: String,
            val initialText: String? = null,
        ) : Params

        @Serializable
        data class ChatInfoParams(
            val chatId: String,
        ) : Params
    }
}
