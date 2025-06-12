package com.security.chat.multiplatform.features.authorize.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.base.BaseComponent
import com.security.chat.multiplatform.common.base.BaseComponentImpl
import com.security.chat.multiplatform.common.base.DiScopeHolder

interface SignInComponent : BaseComponent, DiScopeHolder {

    fun onSignUpClicked()
    fun onSuccessfulSignIn()

}

class SignInComponentImpl(
    private val onSignUp: () -> Unit,
    private val onSignedIn: () -> Unit,
    componentContext: ComponentContext,
) : SignInComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_SIGN_IN,
    ) {

    override fun onSignUpClicked() {
        onSignUp()
    }

    override fun onSuccessfulSignIn() {
        onSignedIn.invoke()
    }

}

const val SCOPE_ID_SIGN_IN = "SCOPE_ID_SIGN_IN"