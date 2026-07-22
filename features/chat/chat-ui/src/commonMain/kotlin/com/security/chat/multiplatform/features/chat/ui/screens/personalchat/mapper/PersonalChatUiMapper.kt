package com.security.chat.multiplatform.features.chat.ui.screens.personalchat.mapper

import com.security.chat.multiplatform.features.chat.domain.entity.Interlocutor
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.MessageDirection
import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.entity.InterlocutorUM
import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.entity.MessageUM
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

private val fullDatetimeFormat = LocalDateTime.Format {
    byUnicodePattern("dd-MM-yyyy HH:mm")
}

private val middleDatetimeFormat = LocalDateTime.Format {
    byUnicodePattern("dd-MM HH:mm")
}

private val shortTimeFormat = LocalDateTime.Format {
    byUnicodePattern("HH:mm")
}

internal fun Message.toUi(): MessageUM {
    val timestampInstant = Instant.fromEpochMilliseconds(timestamp)
    val timestampDatetime = timestampInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    val now = Clock.System.now()
    val nowDatetime = now.toLocalDateTime(TimeZone.currentSystemDefault())

    val datetimeText = when {
        nowDatetime.dayOfYear == timestampDatetime.dayOfYear &&
                nowDatetime.year == timestampDatetime.year -> {
            timestampDatetime.format(shortTimeFormat)
        }

        nowDatetime.year == timestampDatetime.year -> timestampDatetime.format(middleDatetimeFormat)

        else -> timestampDatetime.format(fullDatetimeFormat)
    }

    val messageText = when (this) {
        is Message.Text -> text
        is Message.Image -> "image"
    }

    return when (this.direction) {
        MessageDirection.Incoming -> {
            MessageUM.Incoming(
                id = id,
                text = messageText,
                datetimeText = datetimeText,
            )
        }

        MessageDirection.Outgoing -> {
            MessageUM.Outgoing(
                id = id,
                text = messageText,
                datetimeText = datetimeText,
            )
        }
    }
}

internal fun Interlocutor.toUi(): InterlocutorUM {
    val nameText = if (isOnline) {
        "$name ●"
    } else {
        name
    }

    return InterlocutorUM(
        name = nameText,
        isOnline = isOnline,
    )
}