package com.security.chat.multiplatform.features.add_chat.ui

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.features.add_chat.ui.entity.ChatDescriptor

@Immutable
internal data class AddChatState(
    val groupChat: ChatDescriptor.Group,
    val dialogDescriptor: AlertDialogDescriptor?,
)
