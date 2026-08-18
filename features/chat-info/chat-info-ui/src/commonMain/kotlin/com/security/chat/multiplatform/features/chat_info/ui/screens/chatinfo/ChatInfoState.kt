package com.security.chat.multiplatform.features.chat_info.ui.screens.chatinfo

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.features.chat_info.ui.screens.chatinfo.entity.ChatInfoUM

@Immutable
internal data class ChatInfoState(
    val chatInfoIsLoading: Boolean,
    val chatInfo: ChatInfoUM?,
    val alertDialogDescriptor: AlertDialogDescriptor?,
)
