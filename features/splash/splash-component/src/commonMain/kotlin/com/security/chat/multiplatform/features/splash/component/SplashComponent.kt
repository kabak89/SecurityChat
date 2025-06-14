package com.security.chat.multiplatform.features.splash.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface SplashComponent : BaseComponent, DiScopeHolder {
    public fun onGoAuthorization()
    public fun onUserAuthorized()
}

public class SplashComponentImpl(
    private val goToAuthorize: () -> Unit,
    private val goAuthorizedZone: () -> Unit,
    componentContext: ComponentContext,
) : SplashComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_SPLASH,
    ) {

    override fun onGoAuthorization() {
        goToAuthorize()
    }

    override fun onUserAuthorized() {
        goAuthorizedZone()
    }
}

public const val SCOPE_ID_SPLASH: String = "SCOPE_ID_SPLASH"