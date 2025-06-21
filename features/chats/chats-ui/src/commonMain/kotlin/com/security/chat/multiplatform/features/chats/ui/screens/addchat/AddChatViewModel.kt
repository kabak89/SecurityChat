package com.security.chat.multiplatform.features.chats.ui.screens.addchat

import com.security.chat.multiplatform.common.core.ui.BaseViewModel

internal class AddChatViewModel(
) : BaseViewModel<AddChatState, AddChatEvent>() {

    override fun onPostStart() {
        super.onPostStart()
    }

    override fun createInitialState(): AddChatState {
        return AddChatState()
    }

}