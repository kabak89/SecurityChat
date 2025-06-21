package com.security.chat.multiplatform.features.chats.ui.screens.chatlist

import com.security.chat.multiplatform.common.core.ui.BaseViewModel

internal class ChatListViewModel(
) : BaseViewModel<ChatListState, ChatListEvent>() {

    override fun onPostStart() {
        super.onPostStart()
    }

    override fun createInitialState(): ChatListState {
        return ChatListState()
    }

}