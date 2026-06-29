package com.security.chat.multiplatform.features.chats.ui.screens.addchat

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.features.chats.ui.screens.addchat.entity.ChatDescriptor
import com.security.chat.multiplatform.features.chats.ui.screens.addchat.entity.ChatType

@Immutable
internal data class AddChatState(
    val personalChat: ChatDescriptor.Personal,
    val groupChat: ChatDescriptor.Group,
    val activeType: ChatType,
    val dialogDescriptor: AlertDialogDescriptor?,
)