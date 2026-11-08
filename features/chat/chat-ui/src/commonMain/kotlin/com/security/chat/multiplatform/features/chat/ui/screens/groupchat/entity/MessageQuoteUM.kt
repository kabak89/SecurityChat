package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface MessageQuoteUM {
    val messageId: String

    @Immutable
    data class Text(override val messageId: String, val text: String) : MessageQuoteUM

    @Immutable
    data class Image(override val messageId: String, val filePath: String) : MessageQuoteUM
}
