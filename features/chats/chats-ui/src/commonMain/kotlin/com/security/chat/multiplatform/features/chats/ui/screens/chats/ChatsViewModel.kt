package com.security.chat.multiplatform.features.chats.ui.screens.chats

import com.security.chat.multiplatform.common.core.ui.BaseViewModel

internal class ChatsViewModel(
) : BaseViewModel<ChatsState, ChatsEvent>() {

    override fun onPostStart() {
        super.onPostStart()
    }

    override fun createInitialState(): ChatsState {
        return ChatsState()
    }

}