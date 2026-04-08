package com.security.chat.multiplatform.features.splash.component

import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface SplashComponent : BaseComponent, DiScopeHolder {
    public fun onGoAuthorization()
    public fun onUserAuthorized()
}