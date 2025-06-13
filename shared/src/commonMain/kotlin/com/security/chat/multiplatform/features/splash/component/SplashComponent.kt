package com.security.chat.multiplatform.features.splash.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

interface SplashComponent : BaseComponent, DiScopeHolder {
    fun onGoAuthorization()
    fun onUserAuthorized()
}

class SplashComponentImpl(
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

const val SCOPE_ID_SPLASH = "SCOPE_ID_SPLASH"