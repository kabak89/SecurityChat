package com.security.chat.multiplatform.features.splash.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.splash.data.di.splashDataModule
import com.security.chat.multiplatform.features.splash.domain.SplashModel
import com.security.chat.multiplatform.features.splash.domain.di.splashDomainModule
import com.security.chat.multiplatform.features.splash.ui.di.splashUiModule
import org.koin.core.qualifier.named

public class SplashComponentImpl(
    private val goToAuthorize: () -> Unit,
    private val goAuthorizedZone: () -> Unit,
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
        splashModel.start(parentScope = getKoin().get(named(SCOPE_ID_SPLASH)))

        doOnDestroy {
            getKoin().unloadModules(featureModules)
        }
    }

    override fun onGoAuthorization() {
        goToAuthorize()
    }

    override fun onUserAuthorized() {
        goAuthorizedZone()
    }
}

public const val SCOPE_ID_SPLASH: String = "SCOPE_ID_SPLASH"