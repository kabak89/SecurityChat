package com.security.chat.multiplatform.features.authorize.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.features.authorize.component.api.AuthorizeComponent
import com.security.chat.multiplatform.features.authorize.data.di.authorizeDataModule
import com.security.chat.multiplatform.features.authorize.domain.di.authorizeDomainModule
import com.security.chat.multiplatform.features.settings.ui.di.authorizeUiModule
import kotlinx.serialization.Serializable
import org.koin.mp.KoinPlatform.getKoin

public class AuthorizeComponentImpl(
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

    init {
        val featureModules = listOf(
            authorizeUiModule,
            authorizeDomainModule,
            authorizeDataModule,
        )
        getKoin().loadModules(featureModules)
        doOnDestroy {
            getKoin().unloadModules(featureModules)
        }
    }

    override fun onBackClicked() {
        navigation.pop()
    }

    override fun onCloseClicked() {
        onFinished()
    }

    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): AuthorizeComponent.Child =
        when (params) {
            is Params.SignInParams -> {
                AuthorizeComponent.Child.SignIn(
                    component = SignInComponentImpl(
                        componentContext = componentContext,
                        onSignUp = { navigation.push(Params.SignUpParams) },
                        onSignedIn = onFinished,
                    ),
                )
            }

            is Params.SignUpParams -> {
                AuthorizeComponent.Child.SignUp(
                    component = SignUpComponentImpl(
                        componentContext = componentContext,
                        goToSignIn = { navigation.pop() },
                        onSignedUp = onFinished,
                    ),
                )
            }
        }

    @Serializable
    private sealed class Params {

        @Serializable
        data object SignInParams : Params()

        @Serializable
        data object SignUpParams : Params()

    }

}