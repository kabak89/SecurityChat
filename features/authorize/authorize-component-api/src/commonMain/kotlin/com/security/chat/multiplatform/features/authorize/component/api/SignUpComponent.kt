package com.security.chat.multiplatform.features.authorize.component.api

import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface SignUpComponent : BaseComponent, DiScopeHolder {
    public fun onSignInClicked()
    public fun onSuccessfulSignUp()
}