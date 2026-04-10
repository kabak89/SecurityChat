package com.security.chat.multiplatform.features.profile.component.api

import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface ProfileMainComponent : BaseComponent, DiScopeHolder {

    public fun onBackClicked()
}
