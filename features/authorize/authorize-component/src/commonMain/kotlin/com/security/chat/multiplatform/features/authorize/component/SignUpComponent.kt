package com.security.chat.multiplatform.features.authorize.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.authorize.component.api.SignUpComponent
import com.security.chat.multiplatform.features.authorize.domain.SignUpModel
import org.koin.core.qualifier.named

public class SignUpComponentImpl(
    private val goToSignIn: () -> Unit,
    private val onSignedUp: () -> Unit,
    componentContext: ComponentContext,
) : SignUpComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_SIGN_UP,
    ) {

    init {
        doOnCreate {
            val signUpModel: SignUpModel = getKoin().get()
            signUpModel.start(parentScope = getKoin().get(named(SCOPE_ID_SIGN_UP)))
        }
    }

    override fun onSignInClicked() {
        goToSignIn()
    }

    override fun onSuccessfulSignUp() {
        onSignedUp()
    }

}

public const val SCOPE_ID_SIGN_UP: String = "SCOPE_ID_SIGN_UP"