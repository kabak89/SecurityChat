package com.security.chat.multiplatform.features.chats.component.api

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface ChatsComponent : BaseComponent, DiScopeHolder, BackHandlerOwner {

    public fun onBackClicked()

    public val childStack: Value<ChildStack<*, Child>>

    public sealed interface Child {
        public class ChatList(public val component: ChatListComponent) : Child
        public class AddChat(public val component: AddChatComponent) : Child
    }
}