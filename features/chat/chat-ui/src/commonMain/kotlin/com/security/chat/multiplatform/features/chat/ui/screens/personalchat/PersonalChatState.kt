package com.security.chat.multiplatform.features.chat.ui.screens.personalchat

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.entity.InterlocutorUM
import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.entity.MessageUM

@Immutable
internal data class PersonalChatState(
    val message: String,
    val sendingMessageInProgress: Boolean,
    val messages: List<MessageUM>,
    val syncState: UiLceState,
    val interlocutor: InterlocutorUM?,
)