package com.security.chat.multiplatform.features.chat.component.api

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface ChatComponent : BaseComponent, DiScopeHolder, BackHandlerOwner {

    public val chatId: String

    public fun onBackClicked()

    public val childStack: Value<ChildStack<*, Child>>

    public sealed interface Child {
        public class PersonalChat(public val component: PersonalChatComponent) : Child
    }
}