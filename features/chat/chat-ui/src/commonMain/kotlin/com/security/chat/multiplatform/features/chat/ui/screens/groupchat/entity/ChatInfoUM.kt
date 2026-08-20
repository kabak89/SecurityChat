package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.core.ui.entity.PrintableText

@Immutable
internal data class ChatInfoUM(
    val text: PrintableText,
) {

    companion object {
        fun empty(): ChatInfoUM {
            return ChatInfoUM(
                text = PrintableText.EMPTY,
            )
        }
    }
}
