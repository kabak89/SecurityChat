package com.security.chat.multiplatform.features.chats.component.api

import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface ChatListComponent : BaseComponent, DiScopeHolder {
    public fun onAddClicked()
    public fun onGroupChatClicked(chatId: String)
    public fun onSettingsClicked()
}