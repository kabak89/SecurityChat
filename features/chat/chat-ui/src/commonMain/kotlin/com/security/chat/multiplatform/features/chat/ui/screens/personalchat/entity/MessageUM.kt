package com.security.chat.multiplatform.features.chat.ui.screens.personalchat.entity

import androidx.compose.runtime.Immutable

internal sealed interface MessageUM {

    val id: String

    @Immutable
    data class Outgoing(
        override val id: String,
        val text: String,
    ) : MessageUM

    @Immutable
    data class Incoming(
        override val id: String,
        val text: String,
    ) : MessageUM
}
