package com.security.chat.multiplatform.features.chats.ui.screens.addchat.mapper

import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.entity.UiError
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import securitychat.common.localization.generated.resources.create_chat_error_user_not_found_description
import securitychat.common.localization.generated.resources.create_chat_error_user_not_found_title

internal fun createChatErrorMapper(error: Throwable): UiError? {
    return if (error.isNotFoundError()) {
        UiError(
            title = resPrintableText(StringRes.create_chat_error_user_not_found_title),
            description = resPrintableText(StringRes.create_chat_error_user_not_found_description),
            image = null,
            cause = error,
        )
    } else {
        null
    }
}

internal fun Throwable.isNotFoundError(): Boolean = this is NetworkError && statusCode == 404