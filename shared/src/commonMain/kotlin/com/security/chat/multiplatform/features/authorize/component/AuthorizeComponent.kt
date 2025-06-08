package com.security.chat.multiplatform.features.authorize.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import kotlinx.serialization.Serializable

interface AuthorizeComponent : BackHandlerOwner {

    val childStack: Value<ChildStack<*, Child>>

    fun onBackClicked()

    fun onCloseClicked()

    //child scopes
    sealed interface Child {

        class SignUp(val component: SignUpComponent) : Child
        class SignIn(val component: SignInComponent) : Child

    }
}

class AuthorizeComponentImpl(
    private val onFinished: () -> Unit,
    componentContext: ComponentContext,
) : AuthorizeComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Params>()

    override val childStack: Value<ChildStack<*, AuthorizeComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Params.serializer(),
            initialConfiguration = Params.SignInParams,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override fun onBackClicked() {
        navigation.pop()
    }

    override fun onCloseClicked() {
        onFinished()
    }

    //scope and params corresponding
    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): AuthorizeComponent.Child =
        when (params) {
            is Params.SignInParams -> {
                AuthorizeComponent.Child.SignIn(
                    component = SignInComponentImpl(
                        componentContext = componentContext,
                        onSignUp = {
                            navigation.push(Params.SignUpParams)
                        },
                        onAuthorized = {
                            onFinished()
                        },
                    ),
                )
            }

            is Params.SignUpParams -> {
                AuthorizeComponent.Child.SignUp(
                    component = SignUpComponentImpl(
                        componentContext = componentContext,
                        goToSignIn = {
                            navigation.pop()
                        },
                    ),
                )
            }
        }

    //scope params
    @Serializable
    private sealed class Params {

        @Serializable
        data object SignInParams : Params()

        @Serializable
        data object SignUpParams : Params()

    }

}