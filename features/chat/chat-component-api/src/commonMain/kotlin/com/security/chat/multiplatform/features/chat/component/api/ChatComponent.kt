package com.security.chat.multiplatform.features.chat.component.api

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder
import kotlin.jvm.JvmInline

public interface ChatComponent : BaseComponent, DiScopeHolder, BackHandlerOwner {

    public val params: Params

    public fun onBackClicked()

    public val childStack: Value<ChildStack<*, Child>>

    public sealed interface Child {
        public class PersonalChat(public val component: PersonalChatComponent) : Child
        public class GroupChat(public val component: GroupChatComponent) : Child
    }

    public sealed interface Params {
        @JvmInline
        public value class PersonalChat(public val value: String) : Params

        @JvmInline
        public value class GroupChatId(public val value: String) : Params
    }
}