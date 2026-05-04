package com.security.chat.multiplatform.features.main.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.features.chat.component.ChatComponentImpl
import com.security.chat.multiplatform.features.chat.component.api.ChatComponent
import com.security.chat.multiplatform.features.chats.component.ChatsComponentImpl
import com.security.chat.multiplatform.features.chats.component.api.ChatsComponent
import com.security.chat.multiplatform.features.settings.component.SettingsComponentImpl
import com.security.chat.multiplatform.features.settings.component.api.SettingsComponent
import kotlinx.serialization.Serializable

public interface MainComponent : BackHandlerOwner {

    public fun onBackClicked()

    public fun openChat(chatId: String)

    public val childStack: Value<ChildStack<*, Child>>

    public sealed interface Child {
        public class Chats(public val component: ChatsComponent) : Child
        public class Settings(public val component: SettingsComponent) : Child
        public class Chat(public val component: ChatComponent) : Child
    }
}

public class MainComponentImpl(
    private val onLogout: () -> Unit,
    initialChatId: String? = null,
    componentContext: ComponentContext,
) : MainComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Params>()

    override val childStack: Value<ChildStack<*, MainComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Params.serializer(),
            initialStack = {
                if (initialChatId != null) {
                    listOf(Params.ChatsParams, Params.ChatParams(chatId = initialChatId))
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

    override fun openChat(chatId: String) {
        navigation.navigate { stack ->
            when (val top = stack.last()) {
                is Params.ChatParams -> when {
                    top.chatId == chatId -> stack
                    else -> stack.dropLast(1) + Params.ChatParams(chatId = chatId)
                }

                is Params.SettingsParams -> stack.dropLast(1) + Params.ChatParams(chatId = chatId)
                is Params.ChatsParams -> stack + Params.ChatParams(chatId = chatId)
            }
        }
    }

    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): MainComponent.Child {
        return when (params) {
            is Params.ChatsParams -> {
                MainComponent.Child.Chats(
                    component = ChatsComponentImpl(
                        componentContext = componentContext,
                        onChatClicked = { chatId ->
                            val configuration = Params.ChatParams(
                                chatId = chatId,
                            )
                            navigation.push(configuration = configuration)
                        },
                        onSettingsClicked = {
                            navigation.push(configuration = Params.SettingsParams)
                        },
                    ),
                )
            }

            is Params.SettingsParams -> {
                MainComponent.Child.Settings(
                    component = SettingsComponentImpl(
                        componentContext = componentContext,
                        onExit = navigation::pop,
                        onLogout = onLogout,
                    ),
                )
            }

            is Params.ChatParams -> {
                MainComponent.Child.Chat(
                    component = ChatComponentImpl(
                        componentContext = componentContext,
                        onExit = navigation::pop,
                        chatId = params.chatId,
                    ),
                )
            }
        }
    }

    @Serializable
    private sealed class Params {

        @Serializable
        data object ChatsParams : Params()

        @Serializable
        data object SettingsParams : Params()

        @Serializable
        data class ChatParams(
            val chatId: String,
        ) : Params()
    }
}
