package com.security.chat.multiplatform.features.chat.ui.screens.groupchat

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState

@Immutable
internal data class GroupChatState(
    val message: String,
    val syncState: UiLceState,
)
