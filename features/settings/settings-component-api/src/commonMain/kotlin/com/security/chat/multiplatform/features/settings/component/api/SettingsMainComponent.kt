package com.security.chat.multiplatform.features.settings.component.api

import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface SettingsMainComponent : BaseComponent, DiScopeHolder {

    public fun onExitClicked()
    public fun onUserLogOuted()
    public fun onGoToThemeClicked()

}