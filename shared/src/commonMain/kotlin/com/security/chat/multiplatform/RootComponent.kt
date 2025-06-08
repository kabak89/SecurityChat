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
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.features.authorize.component.AuthorizeComponent
import com.security.chat.multiplatform.features.authorize.component.DefaultAuthorizeComponent
import com.security.chat.multiplatform.features.authorize.ui.screens.authorize.AuthorizeScreen
import com.security.chat.multiplatform.features.main.component.DefaultMainComponent
import com.security.chat.multiplatform.features.main.component.MainComponent
import com.security.chat.multiplatform.features.main.ui.screens.main.MainScreen
import com.security.chat.multiplatform.features.splash.component.SplashComponent
import com.security.chat.multiplatform.features.splash.component.SplashComponentImpl
import com.security.chat.multiplatform.features.splash.ui.screens.splash.SplashScreen
import kotlinx.serialization.Serializable
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

interface RootComponent : BackHandlerOwner {
    val childStack: Value<ChildStack<*, Child>>

    fun onBackClicked()

    //child scopes
    sealed interface Child {

        class Splash(val component: SplashComponent) : Child
        class Authorize(val component: AuthorizeComponent) : Child
        class Main(val component: MainComponent) : Child

    }
}

class RootComponentImpl(
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {

    init {
        println("ewqeqweqw RootComponentImpl doOnCreate")
        startKoin {
            modules(diModules)
        }
//        lifecycle.doOnCreate {
//        }

        lifecycle.doOnDestroy {
            println("ewqeqweqw RootComponentImpl doOnDestroy")
            stopKoin()
        }
    }

    private val navigation: StackNavigation<Params> = StackNavigation()

    override val childStack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Params.serializer(),
            initialConfiguration = Params.Splash,
            handleBackButton = true,
            childFactory = { params, componentContext ->
                createChild(
                    params = params,
                    componentContext = componentContext,
                )
            },
        )

    override fun onBackClicked() {
        navigation.pop()
    }

    //scope and params corresponding
    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): RootComponent.Child {
        return when (params) {
            is Params.Splash -> {
                RootComponent.Child.Splash(
                    component = createSplashComponent(componentContext = componentContext),
                )
            }

            is Params.Authorize -> {
                RootComponent.Child.Authorize(
                    component = createAuthorizeComponent(componentContext = componentContext),
                )
            }

            Params.Main -> {
                RootComponent.Child.Main(
                    component = createMainComponent(componentContext = componentContext),
                )
            }
        }
    }

    private fun createMainComponent(componentContext: ComponentContext): MainComponent {
        return DefaultMainComponent(
            componentContext = componentContext,
        )
    }

    private fun createSplashComponent(
        componentContext: ComponentContext,
    ): SplashComponent {
        return SplashComponentImpl(
            componentContext = componentContext,
            goToAuthorize = { navigation.replaceAll(Params.Authorize) },
            goAuthorizedZone = { navigation.replaceAll(Params.Main) },
        )
    }

    private fun createAuthorizeComponent(
        componentContext: ComponentContext,
    ): AuthorizeComponent {
        return DefaultAuthorizeComponent(
            componentContext = componentContext,
            onFinished = { navigation.replaceAll(Params.Main) },
        )
    }

    //scope params
    @Serializable
    private sealed interface Params {

        @Serializable
        data object Splash : Params

        @Serializable
        data object Authorize : Params

        @Serializable
        data object Main : Params

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
            is RootComponent.Child.Main -> MainScreen(component = child.component)
        }
    }
}