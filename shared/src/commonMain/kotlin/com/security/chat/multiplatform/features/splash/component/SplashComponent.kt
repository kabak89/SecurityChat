package com.security.chat.multiplatform.features.splash.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.base.BaseComponent
import com.security.chat.multiplatform.common.base.BaseComponentImpl

interface SplashComponent : BaseComponent {
    fun onAuthorizeClicked()
}

class SplashComponentImpl(
    private val goToAuthorize: () -> Unit,
    componentContext: ComponentContext,
) : SplashComponent, BaseComponentImpl(componentContext = componentContext) {

    override fun onAuthorizeClicked() {
        goToAuthorize()
    }

}