package com.security.chat.multiplatform.features.main.component.api

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.features.add_chat.component.api.AddChatComponent
import com.security.chat.multiplatform.features.chat.component.api.ChatComponent
import com.security.chat.multiplatform.features.chats.component.api.ChatsComponent
import com.security.chat.multiplatform.features.settings.component.api.SettingsComponent

public interface MainComponent : BackHandlerOwner {

    public fun onBackClicked()
    public fun openGroupChat(chatId: String)
    public fun handleSendText(text: String)

    public val childStack: Value<ChildStack<*, Child>>

    public sealed interface Child {
        public class Chats(public val component: ChatsComponent) : Child
        public class Chat(public val component: ChatComponent) : Child
        public class Settings(public val component: SettingsComponent) : Child
        public class AddChat(public val component: AddChatComponent) : Child
    }

    public sealed interface Params {
        public data class GroupChat(val chatId: String) : Params
        public data class ShareText(val text: String) : Params
    }
}
