package com.security.chat.multiplatform

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.DiScopeHolder
import com.security.chat.multiplatform.common.core.component.SCOPE_ID_ROOT
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.di.diModules
import com.security.chat.multiplatform.features.authorize.component.AuthorizeComponent
import com.security.chat.multiplatform.features.authorize.component.AuthorizeComponentImpl
import com.security.chat.multiplatform.features.authorize.ui.screens.authorize.AuthorizeScreen
import com.security.chat.multiplatform.features.main.component.MainComponent
import com.security.chat.multiplatform.features.main.component.MainComponentImpl
import com.security.chat.multiplatform.features.main.ui.screens.main.MainScreen
import com.security.chat.multiplatform.features.settings.data.storage.SettingsStorage
import com.security.chat.multiplatform.features.settings.data.storage.entity.ThemeSM
import com.security.chat.multiplatform.features.splash.component.SplashComponent
import com.security.chat.multiplatform.features.splash.component.SplashComponentImpl
import com.security.chat.multiplatform.features.splash.ui.screens.splash.SplashScreen
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.Serializable
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module

interface RootComponent : BackHandlerOwner, DiScopeHolder {
    val childStack: Value<ChildStack<*, Child>>

    fun onBackClicked()

    sealed interface Child {

        class Splash(val component: SplashComponent) : Child
        class Authorize(val component: AuthorizeComponent) : Child
        class Main(val component: MainComponent) : Child

    }
}

class RootComponentImpl(
    private val onCreate: () -> Unit = {},
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {

    private var diScope: Scope? = null

    init {
        println("RootComponentImpl doOnCreate")
        startKoin {
            modules(diModules)
        }

        diScope = getKoin().createScope(
            scopeId = SCOPE_ID_ROOT,
            qualifier = named(SCOPE_ID_ROOT),
        )

        println("scope $SCOPE_ID_ROOT created")

        val coroutineScopeModule = module {
            scope(named(SCOPE_ID_ROOT)) {
                scoped {
                    val errorHandler = CoroutineExceptionHandler { _, e ->
                        println("error in coroutine scope in $SCOPE_ID_ROOT DI scope: $e")
                    }

                    val dispatcherProvider: DispatcherProviderInterface = get()
                    CoroutineScope(
                        dispatcherProvider.IO +
                                SupervisorJob() +
                                errorHandler +
                                CoroutineName(SCOPE_ID_ROOT),
                    )
                } bind CoroutineScope::class
            }
        }

        loadKoinModules(coroutineScopeModule)

        lifecycle.doOnCreate {
            onCreate()
        }

        lifecycle.doOnDestroy {
            println("RootComponentImpl doOnDestroy")

            val scopedCoroutineScope: CoroutineScope = diScope!!.get()
            scopedCoroutineScope.cancel()

            diScope?.close()
            println("scope $SCOPE_ID_ROOT closed")

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

    override fun getDiScope(): Scope {
        return diScope!!
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
        return MainComponentImpl(
            componentContext = componentContext,
            onLogout = { navigation.replaceAll(Params.Authorize) },
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
        return AuthorizeComponentImpl(
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

@Composable
fun RootContent(rootComponent: RootComponent) {
    val settingsStorage: SettingsStorage = rootComponent.getKoin().get()
    val theme = settingsStorage.getCurrentThemeFlow().collectAsState(ThemeSM.Auto).value

    val useDarkTheme = when (theme) {
        ThemeSM.Auto -> isSystemInDarkTheme()
        ThemeSM.Dark -> true
        ThemeSM.Light -> false
    }

    AppTheme(
        useDarkTheme = useDarkTheme,
    ) {
        Children(
            stack = rootComponent.childStack,
            animation = predictiveBackAnimation(
                backHandler = rootComponent.backHandler,
                fallbackAnimation = stackAnimation(slide()),
                onBack = rootComponent::onBackClicked,
            ),
            content = {
                when (val child = it.instance) {
                    is RootComponent.Child.Splash -> SplashScreen(component = child.component)
                    is RootComponent.Child.Authorize -> AuthorizeScreen(component = child.component)
                    is RootComponent.Child.Main -> {
                        MainScreen(
                            component = child.component,
                        )
                    }
                }
            },
        )
    }
}