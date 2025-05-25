package com.security.chat.multiplatform

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.features.authorize.component.AuthorizeComponent
import com.security.chat.multiplatform.features.authorize.component.DefaultAuthorizeComponent
import com.security.chat.multiplatform.features.authorize.ui.screens.authorize.AuthorizeScreen
import com.security.chat.multiplatform.features.splash.component.DefaultSplashComponent
import com.security.chat.multiplatform.features.splash.component.SplashComponent
import com.security.chat.multiplatform.features.splash.ui.screens.splash.SplashScreen
import kotlinx.serialization.Serializable

interface RootComponent : BackHandlerOwner {
    val childStack: Value<ChildStack<*, Child>>

    fun onBackClicked()

    //child scopes
    sealed interface Child {

        class Splash(val component: SplashComponent) : Child
        class Authorize(val component: AuthorizeComponent) : Child

    }
}

class DefaultRootComponent(
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Params>()

    override val childStack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Params.serializer(),
            initialConfiguration = Params.Splash,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override fun onBackClicked() {
        navigation.pop()
    }

    //scope and params corresponding
    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): RootComponent.Child =
        when (params) {
            is Params.Splash -> {
                RootComponent.Child.Splash(
                    component = itemList(componentContext),
                )
            }

            is Params.Authorize -> {
                RootComponent.Child.Authorize(
                    itemDetails(
                        componentContext = componentContext,
                    ),
                )
            }
        }

    private fun itemList(
        componentContext: ComponentContext,
    ): SplashComponent {
        return DefaultSplashComponent(
            componentContext = componentContext,
            onItemSelected = { navigation.push(Params.Authorize) },
            goToAuthorize = { navigation.push(Params.Authorize) },
        )
    }

    private fun itemDetails(
        componentContext: ComponentContext,
    ): AuthorizeComponent {
        return DefaultAuthorizeComponent(
            componentContext = componentContext,
            onFinished = { navigation.pop() },
        )
    }

    //scope params
    @Serializable
    private sealed interface Params {

        @Serializable
        data object Splash : Params

        @Serializable
        data object Authorize : Params

    }

}

//initial UI
@Composable
fun RootContent(rootComponent: RootComponent) {
    //scope and UI corresponding
    Children(
        stack = rootComponent.childStack,
        animation = predictiveBackAnimation(
            backHandler = rootComponent.backHandler,
            fallbackAnimation = stackAnimation(slide()),
            onBack = rootComponent::onBackClicked,
        ),
    ) {
        when (val child = it.instance) {
            is RootComponent.Child.Splash -> SplashScreen(component = child.component)
            is RootComponent.Child.Authorize -> AuthorizeScreen(component = child.component)
        }
    }
}