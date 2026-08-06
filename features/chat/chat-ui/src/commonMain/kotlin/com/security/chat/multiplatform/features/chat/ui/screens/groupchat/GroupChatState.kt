package com.security.chat.multiplatform.features.chat.ui.screens.groupchat

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.FullscreenImageUM

@Immutable
internal data class GroupChatState(
    val message: String,
    val syncState: UiLceState,
    val alertDialogDescriptor: AlertDialogDescriptor?,
    val fullscreenImage: FullscreenImageUM?,
)
