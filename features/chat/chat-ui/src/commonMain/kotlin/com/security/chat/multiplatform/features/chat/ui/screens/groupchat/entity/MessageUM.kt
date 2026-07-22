package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.features.chat.ui.screens.common.entity.ItemWithId

internal sealed interface MessageUM : ItemWithId {

    override val id: String
    val text: String
    val datetimeText: String

    sealed interface Outgoing : MessageUM {
        override val id: String

        @Immutable
        data class Text(
            override val id: String,
            override val text: String,
            override val datetimeText: String,
        ) : Outgoing

        @Immutable
        data class Image(
            override val id: String,
            override val text: String,
            override val datetimeText: String,
        ) : Outgoing
    }

    sealed interface Incoming : MessageUM {
        override val id: String

        @Immutable
        data class Text(
            override val id: String,
            override val text: String,
            override val datetimeText: String,
            val senderName: String,
        ) : Incoming

        @Immutable
        data class Image(
            override val id: String,
            override val text: String,
            override val datetimeText: String,
        ) : Incoming
    }
}
