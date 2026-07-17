package com.security.chat.multiplatform.features.splash.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.splash.data.di.splashDataModule
import com.security.chat.multiplatform.features.splash.domain.SplashModel
import com.security.chat.multiplatform.features.splash.domain.di.splashDomainModule
import com.security.chat.multiplatform.features.splash.ui.di.splashUiModule

public class SplashComponentImpl(
    private val onSplashFinished: (userState: UserState) -> Unit,
    componentContext: ComponentContext,
) : SplashComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_SPLASH,
    ) {

    init {
        val featureModules = listOf(
            splashDomainModule,
            splashUiModule,
            splashDataModule,
        )
        getKoin().loadModules(featureModules)

        val splashModel: SplashModel = getKoin().get()
        splashModel.start(parentScope = componentCoroutineScope)

        doOnDestroy {
            getKoin().unloadModules(featureModules)
        }
    }

    override fun onFinished(userState: UserState) {
        onSplashFinished(userState)
    }
}

public const val SCOPE_ID_SPLASH: String = "SCOPE_ID_SPLASH"