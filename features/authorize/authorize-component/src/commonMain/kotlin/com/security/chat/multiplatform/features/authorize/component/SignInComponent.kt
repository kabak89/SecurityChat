package com.security.chat.multiplatform.features.authorize.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.authorize.component.api.SignInComponent
import com.security.chat.multiplatform.features.authorize.component.api.UserState
import com.security.chat.multiplatform.features.authorize.domain.SignInModel
import org.koin.core.qualifier.named

public class SignInComponentImpl(
    private val onSignUp: () -> Unit,
    private val onSignedIn: (userState: UserState) -> Unit,
    componentContext: ComponentContext,
) : SignInComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_SIGN_IN,
    ) {

    init {
        doOnCreate {
            val signInModel: SignInModel = getKoin().get()
            signInModel.start(parentScope = getKoin().get(named(SCOPE_ID_SIGN_IN)))
        }
    }

    override fun onSignUpClicked() {
        onSignUp()
    }

    override fun onSuccessfulSignIn(userState: UserState) {
        onSignedIn(userState)
    }

}

public const val SCOPE_ID_SIGN_IN: String = "SCOPE_ID_SIGN_IN"