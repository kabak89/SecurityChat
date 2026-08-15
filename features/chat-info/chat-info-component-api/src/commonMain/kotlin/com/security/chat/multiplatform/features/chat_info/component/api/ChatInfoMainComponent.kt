package com.security.chat.multiplatform.features.chat_info.component.api

import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface ChatInfoMainComponent : BaseComponent, DiScopeHolder {
    public val chatId: String

    public fun onBackClicked()
    public fun onAddMembersClicked()
}
