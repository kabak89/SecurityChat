package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.mapper

import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.entity.UiError
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.features.chat.domain.entity.error.NotImageError
import securitychat.common.localization.generated.resources.group_chat_error_not_image_description
import securitychat.common.localization.generated.resources.group_chat_error_not_image_title

internal fun groupChatErrorMapper(error: Throwable): UiError? {
    return when {
        error.isNotImageError() -> {
            UiError(
                title = resPrintableText(StringRes.group_chat_error_not_image_title),
                description = resPrintableText(StringRes.group_chat_error_not_image_description),
                image = null,
                cause = error,
            )
        }

        else -> {
            null
        }
    }
}

internal fun Throwable.isNotImageError(): Boolean = this is NotImageError