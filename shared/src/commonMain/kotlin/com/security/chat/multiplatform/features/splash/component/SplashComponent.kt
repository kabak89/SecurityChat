package com.security.chat.multiplatform.features.splash.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.base.BaseComponent
import com.security.chat.multiplatform.common.base.BaseComponentImpl
import com.security.chat.multiplatform.common.base.DiScopeHolder

interface SplashComponent : BaseComponent, DiScopeHolder {
    fun onAuthorizeClicked()
}

class SplashComponentImpl(
    private val goToAuthorize: () -> Unit,
    componentContext: ComponentContext,
) : SplashComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_SPLASH,
    ) {

    override fun onAuthorizeClicked() {
        goToAuthorize()
    }

}

internal const val SCOPE_ID_SPLASH = "splash"