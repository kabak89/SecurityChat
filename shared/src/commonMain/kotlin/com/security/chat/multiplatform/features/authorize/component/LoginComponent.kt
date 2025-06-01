package com.security.chat.multiplatform.features.authorize.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.base.BaseComponent
import com.security.chat.multiplatform.common.base.BaseComponentImpl

interface LoginComponent : BaseComponent {
    fun onSignInClicked()
}

class LoginComponentImpl(
    private val goToSignIn: () -> Unit,
    componentContext: ComponentContext,
) : LoginComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        //TODO
        scopeId = "",
    ) {

    override fun onSignInClicked() {
        goToSignIn()
    }

}