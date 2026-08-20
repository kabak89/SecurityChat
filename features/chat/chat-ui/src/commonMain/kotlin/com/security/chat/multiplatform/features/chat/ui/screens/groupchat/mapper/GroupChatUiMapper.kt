package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.mapper

import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.features.chat.domain.entity.ChatInfo
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.MessageDirection
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.ChatInfoUM
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import securitychat.common.localization.generated.resources.chat_online_members_template
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

    return when (this) {
        is Message.Text -> when (direction) {
            MessageDirection.Incoming -> MessageUM.Incoming.Text(
                id = id,
                text = text,
                datetimeText = datetimeText,
                senderName = author.name,
            )

            MessageDirection.Outgoing -> MessageUM.Outgoing.Text(
                id = id,
                text = text,
                datetimeText = datetimeText,
            )
        }

        is Message.Image -> when (direction) {
            MessageDirection.Incoming -> {
                val localFilePath = filePath

                if (localFilePath == null) {
                    return MessageUM.Nothing(
                        id = id,
                        text = "",
                        datetimeText = datetimeText,
                    )
                }

                MessageUM.Incoming.Image(
                    id = id,
                    text = "image",
                    datetimeText = datetimeText,
                    filePath = localFilePath,
                    senderName = author.name,
                )
            }

            MessageDirection.Outgoing -> {
                val localFilePath = filePath

                if (localFilePath == null) {
                    return MessageUM.Nothing(
                        id = id,
                        text = "",
                        datetimeText = datetimeText,
                    )
                }

                MessageUM.Outgoing.Image(
                    id = id,
                    text = "image",
                    datetimeText = datetimeText,
                    filePath = localFilePath,
                )
            }
        }
    }
}

internal fun ChatInfo.toUi(): ChatInfoUM {
    return ChatInfoUM(
        text = resPrintableText(
            StringRes.chat_online_members_template,
            this.onlineCount,
            this.totalMembersCount,
        ),
    )
}