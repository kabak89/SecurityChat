package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.mapper

import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.MessageDirection
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

private val fullDatetimeFormat = LocalDateTime.Format {
    byUnicodePattern("yyyy-MM-dd HH:mm")
}

private val middleDatetimeFormat = LocalDateTime.Format {
    byUnicodePattern("MM-dd HH:mm")
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

    return when (this.direction) {
        MessageDirection.Incoming -> {
            MessageUM.Incoming(
                id = id,
                text = text,
                datetimeText = datetimeText,
            )
        }

        MessageDirection.Outgoing -> {
            MessageUM.Outgoing(
                id = id,
                text = text,
                datetimeText = datetimeText,
            )
        }
    }
}