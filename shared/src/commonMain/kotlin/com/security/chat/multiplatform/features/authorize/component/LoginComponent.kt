package com.security.chat.multiplatform.features.authorize.component

import com.arkivanov.decompose.ComponentContext

interface LoginComponent {
    fun onSignInClicked()
}

class DefaultLoginComponent(
    componentContext: ComponentContext,
    private val gotToSignIn: () -> Unit,
) : LoginComponent, ComponentContext by componentContext {

    override fun onSignInClicked() {
        gotToSignIn()
    }

}