package com.security.chat.multiplatform.features.authorize.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.base.BaseComponent
import com.security.chat.multiplatform.common.base.BaseComponentImpl

interface SignUpComponent : BaseComponent {
    fun onSignInClicked()
}

class SignUpComponentImpl(
    private val goToSignIn: () -> Unit,
    componentContext: ComponentContext,
) : SignUpComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        //TODO
        scopeId = "",
    ) {

    override fun onSignInClicked() {
        goToSignIn()
    }

}