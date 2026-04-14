package com.security.chat.multiplatform.features.chats.ui.screens.chatlist

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.ui.kit.alertdialog.AlertDialogContent
import com.security.chat.multiplatform.features.chats.ui.screens.chatlist.entity.Chats

@Immutable
internal data class ChatListState(
    val loadingState: UiLceState,
    val chats: Chats,
    val errorDialogContent: AlertDialogContent?,
)