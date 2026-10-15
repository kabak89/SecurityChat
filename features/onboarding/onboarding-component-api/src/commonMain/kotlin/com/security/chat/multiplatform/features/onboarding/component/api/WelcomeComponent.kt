package com.security.chat.multiplatform.features.onboarding.component.api

import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface WelcomeComponent : BaseComponent, DiScopeHolder {
    public fun onContinue()

    public fun onFinish()
}
