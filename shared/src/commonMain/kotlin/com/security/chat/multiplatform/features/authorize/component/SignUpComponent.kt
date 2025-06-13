package com.security.chat.multiplatform.features.authorize.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

interface SignUpComponent : BaseComponent, DiScopeHolder {
    fun onSignInClicked()
    fun onSuccessfulSignUp()
}

class SignUpComponentImpl(
    private val goToSignIn: () -> Unit,
    private val onSignedUp: () -> Unit,
    componentContext: ComponentContext,
) : SignUpComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_SIGN_UP,
    ) {

    override fun onSignInClicked() {
        goToSignIn()
    }

    override fun onSuccessfulSignUp() {
        onSignedUp()
    }

}

const val SCOPE_ID_SIGN_UP = "SCOPE_ID_SIGN_UP"