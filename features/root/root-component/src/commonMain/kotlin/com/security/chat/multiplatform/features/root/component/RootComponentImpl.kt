package com.security.chat.multiplatform.features.root.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.SCOPE_ID_UI
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.authorize.component.AuthorizeComponentImpl
import com.security.chat.multiplatform.features.authorize.component.api.AuthorizeComponent
import com.security.chat.multiplatform.features.main.component.MainComponent
import com.security.chat.multiplatform.features.main.component.MainComponentImpl
import com.security.chat.multiplatform.features.onboarding.component.OnboardingComponentImpl
import com.security.chat.multiplatform.features.onboarding.component.api.OnboardingComponent
import com.security.chat.multiplatform.features.push.domain.PushModel
import com.security.chat.multiplatform.features.root.component.api.RootComponent
import com.security.chat.multiplatform.features.splash.component.SplashComponent
import com.security.chat.multiplatform.features.splash.component.SplashComponentImpl
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module

public class RootComponentImpl(
    private val onCreate: () -> Unit = {},
    initialDeepLink: RootComponent.DeepLink? = null,
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {

    private var diScope: Scope? = null

    private var pendingChatId: String? =
        (initialDeepLink as? RootComponent.DeepLink.OpenChat)?.chatId

    init {
        Log.d { "RootComponentImpl doOnCreate" }

        diScope = getKoin().createScope(
            scopeId = SCOPE_ID_UI,
            qualifier = named(SCOPE_ID_UI),
        )

        Log.d { "scope $SCOPE_ID_UI created" }

        val coroutineScopeModule = module {
            single(named(SCOPE_ID_UI)) {
                val errorHandler = CoroutineExceptionHandler { _, e ->
                    Log.e(e, "error in coroutine scope in $SCOPE_ID_UI DI scope")
                }

                val dispatcherProvider: DispatcherProviderInterface = get()
                CoroutineScope(
                    dispatcherProvider.IO +
                            SupervisorJob() +
                            errorHandler +
                            CoroutineName(SCOPE_ID_UI),
                )
            } bind CoroutineScope::class
        }

        loadKoinModules(
            listOf(
                coroutineScopeModule,
            ),
        )

        lifecycle.doOnCreate {
            onCreate()

            val pushModel: PushModel = getKoin().get()
            val rootCoroutineScope: CoroutineScope = getKoin().get(named(SCOPE_ID_UI))
            rootCoroutineScope.launch {
                pushModel.registerCurrentToken()
            }
        }

        lifecycle.doOnDestroy {
            Log.d { "RootComponentImpl doOnDestroy" }

            val scopedCoroutineScope: CoroutineScope = getKoin().get(named(SCOPE_ID_UI))
            scopedCoroutineScope.cancel()

            diScope?.close()
            Log.d { "scope $SCOPE_ID_UI closed" }
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

    override fun handleDeepLink(link: RootComponent.DeepLink) {
        when (link) {
            is RootComponent.DeepLink.OpenChat -> {
                val activeMain = childStack.value.active.instance as? RootComponent.Child.Main
                if (activeMain != null) {
                    activeMain.component.openChat(chatId = link.chatId)
                } else {
                    pendingChatId = link.chatId
                }
            }
        }
    }

    override fun getDiScope(): Scope {
        return diScope!!
    }

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

            Params.Onboarding -> {
                RootComponent.Child.Onboarding(
                    component = createOnboardingComponent(componentContext = componentContext),
                )
            }
        }
    }

    private fun createMainComponent(componentContext: ComponentContext): MainComponent {
        val chatId = pendingChatId
        pendingChatId = null
        return MainComponentImpl(
            componentContext = componentContext,
            onLogout = { navigation.replaceAll(Params.Authorize) },
            initialChatId = chatId,
        )
    }

    private fun createSplashComponent(
        componentContext: ComponentContext,
    ): SplashComponent {
        return SplashComponentImpl(
            componentContext = componentContext,
            onSplashFinished = { state ->
                when {
                    !state.isAuthorized -> navigation.replaceAll(Params.Authorize)
                    !state.isOnboardingPassed -> navigation.replaceAll(Params.Onboarding)
                    else -> navigation.replaceAll(Params.Main)
                }
            },
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

    private fun createOnboardingComponent(
        componentContext: ComponentContext,
    ): OnboardingComponent {
        return OnboardingComponentImpl(
            componentContext = componentContext,
            onFinished = { navigation.replaceAll(Params.Main) },
        )
    }

    @Serializable
    private sealed interface Params {

        @Serializable
        data object Splash : Params

        @Serializable
        data object Authorize : Params

        @Serializable
        data object Main : Params

        @Serializable
        data object Onboarding : Params
    }
}