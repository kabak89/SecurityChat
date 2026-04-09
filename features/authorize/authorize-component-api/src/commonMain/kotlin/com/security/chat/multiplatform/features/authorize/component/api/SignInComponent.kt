package com.security.chat.multiplatform.features.authorize.component.api

import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface SignInComponent : BaseComponent, DiScopeHolder {
    public fun onSignUpClicked()
    public fun onSuccessfulSignIn()
}