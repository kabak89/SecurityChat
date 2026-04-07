package com.security.chat.multiplatform.features.authorize.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.common.core.component.DiScopeHolder
import com.security.chat.multiplatform.features.authorize.domain.SignInModel
import kotlinx.coroutines.CoroutineScope
import org.koin.core.qualifier.named

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

    init {
        doOnCreate {
            val signInModel: SignInModel = getKoin().get()
            val coroutineScope: CoroutineScope = getKoin().get(named(SCOPE_ID_SIGN_IN))
            signInModel.start(parentScope = coroutineScope)
        }
    }

    override fun onSignUpClicked() {
        onSignUp()
    }

    override fun onSuccessfulSignIn() {
        onSignedIn()
    }

}

const val SCOPE_ID_SIGN_IN = "SCOPE_ID_SIGN_IN"