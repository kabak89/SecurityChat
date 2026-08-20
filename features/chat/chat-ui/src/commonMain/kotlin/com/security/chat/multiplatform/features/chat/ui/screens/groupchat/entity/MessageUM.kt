package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity

import androidx.compose.runtime.Immutable

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
            val filePath: String,
        ) : Outgoing
    }

    sealed interface Incoming : MessageUM {
        override val id: String
        val senderName: String

        @Immutable
        data class Text(
            override val id: String,
            override val text: String,
            override val datetimeText: String,
            override val senderName: String,
        ) : Incoming

        @Immutable
        data class Image(
            override val id: String,
            override val text: String,
            override val datetimeText: String,
            override val senderName: String,
            val filePath: String,
        ) : Incoming
    }

    @Immutable
    data class Nothing(
        override val id: String,
        override val text: String,
        override val datetimeText: String,
    ) : MessageUM
}
