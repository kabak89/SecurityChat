package com.security.chat.multiplatform.features.add_chat.component.api

import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface AddChatComponent : BaseComponent, DiScopeHolder {
    public fun onBackClicked()
    public fun onGroupChatCreated(chatId: String)
}
