package com.security.chat.multiplatform.features.chat.component.api

import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface PersonalChatComponent : BaseComponent, DiScopeHolder {
    public val chatId: String

    public fun onExitClicked()
}