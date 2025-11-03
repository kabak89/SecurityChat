package com.security.chat.multiplatform.features.main.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.features.chat.component.ChatComponent
import com.security.chat.multiplatform.features.chat.component.ChatComponentImpl
import com.security.chat.multiplatform.features.chats.component.ChatsComponent
import com.security.chat.multiplatform.features.chats.component.ChatsComponentImpl
import com.security.chat.multiplatform.features.settings.component.SettingsComponent
import com.security.chat.multiplatform.features.settings.component.SettingsComponentImpl
import kotlinx.serialization.Serializable

public interface MainComponent : BackHandlerOwner {

    public fun onBackClicked()

    public val childStack: Value<ChildStack<*, Child>>

    public sealed interface Child {

        public class Chats(public val component: ChatsComponent) : Child
        public class Settings(public val component: SettingsComponent) : Child
        public class Chat(public val component: ChatComponent) : Child

    }

}

public class MainComponentImpl(
    private val onLogout: () -> Unit,
    componentContext: ComponentContext,
) : MainComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Params>()

    override val childStack: Value<ChildStack<*, MainComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Params.serializer(),
            initialConfiguration = Params.ChatsParams,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override fun onBackClicked() {
        navigation.pop()
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