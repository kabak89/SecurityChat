package com.security.chat.multiplatform.features.call.component.api

import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface CallMainComponent : BaseComponent, DiScopeHolder {
    public val chatId: String

    public fun onBackClicked()
}
