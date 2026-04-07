package com.security.chat.multiplatform.features.settings.component.api

import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface ThemeComponent : BaseComponent, DiScopeHolder {

    public fun onBackClicked()

}