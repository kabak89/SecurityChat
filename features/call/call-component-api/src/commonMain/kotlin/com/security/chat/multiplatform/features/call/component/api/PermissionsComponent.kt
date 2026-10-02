package com.security.chat.multiplatform.features.call.component.api

import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface PermissionsComponent : BaseComponent, DiScopeHolder {

    public fun onBackClicked()

    public fun onPermissionsGranted()
}
