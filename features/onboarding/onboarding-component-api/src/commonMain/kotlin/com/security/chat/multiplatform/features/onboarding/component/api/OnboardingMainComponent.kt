package com.security.chat.multiplatform.features.onboarding.component.api

import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface OnboardingMainComponent : BaseComponent, DiScopeHolder {
    public fun onFinish()
}
