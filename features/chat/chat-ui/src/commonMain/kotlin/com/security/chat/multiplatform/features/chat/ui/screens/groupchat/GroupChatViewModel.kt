package com.security.chat.multiplatform.features.chat.ui.screens.groupchat

import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.chat.component.api.GroupChatComponent

internal class GroupChatViewModel(
    private val params: GroupChatComponent,
) : BaseViewModel<GroupChatState, GroupChatEvent>() {

    override fun createInitialState(): GroupChatState = GroupChatState()
}
