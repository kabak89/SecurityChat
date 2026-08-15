package com.security.chat.multiplatform.features.chat_info.ui.screens.chatinfo

import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.chat_info.component.api.ChatInfoMainComponent

internal class ChatInfoViewModel(
    val params: ChatInfoMainComponent,
) : BaseViewModel<ChatInfoState, Unit>() {

    override fun createInitialState(): ChatInfoState = ChatInfoState(
        title = "Chat Info: ${params.chatId}",
    )
}
