package com.security.chat.multiplatform.features.chat_info.component.api

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface ChatInfoComponent : BaseComponent, DiScopeHolder, BackHandlerOwner {
    public val chatId: String

    public val childStack: Value<ChildStack<*, Child>>

    public fun onBackClicked()

    public sealed interface Child {
        public class ChatInfoMain(public val component: ChatInfoMainComponent) : Child
        public class AddMember(public val component: AddMemberComponent) : Child
    }
}
