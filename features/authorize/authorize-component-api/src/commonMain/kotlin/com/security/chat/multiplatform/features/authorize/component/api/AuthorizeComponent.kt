package com.security.chat.multiplatform.features.authorize.component.api

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner

public interface AuthorizeComponent : BackHandlerOwner {

    public val childStack: Value<ChildStack<*, Child>>

    public fun onBackClicked()
    public fun onCloseClicked()

    public sealed interface Child {
        public class SignUp(public val component: SignUpComponent) : Child
        public class SignIn(public val component: SignInComponent) : Child
    }
}