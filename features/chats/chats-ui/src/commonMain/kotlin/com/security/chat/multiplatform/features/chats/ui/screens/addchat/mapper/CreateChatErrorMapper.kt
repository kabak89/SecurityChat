package com.security.chat.multiplatform.features.chats.ui.screens.addchat.mapper

import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.entity.UiError
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.features.chats.domain.entity.SameUserError
import securitychat.common.localization.generated.resources.create_chat_error_same_user_description
import securitychat.common.localization.generated.resources.create_chat_error_same_user_title
import securitychat.common.localization.generated.resources.create_chat_error_user_not_found_description
import securitychat.common.localization.generated.resources.create_chat_error_user_not_found_title

internal fun createChatErrorMapper(error: Throwable): UiError? {
    return when {
        error.isNotFoundError() -> {
            UiError(
                title = resPrintableText(StringRes.create_chat_error_user_not_found_title),
                description = resPrintableText(StringRes.create_chat_error_user_not_found_description),
                image = null,
                cause = error,
            )
        }

        error.isSameUserError() -> {
            UiError(
                title = resPrintableText(StringRes.create_chat_error_same_user_title),
                description = resPrintableText(StringRes.create_chat_error_same_user_description),
                image = null,
                cause = error,
            )
        }

        else -> {
            null
        }
    }
}

internal fun Throwable.isNotFoundError(): Boolean = this is NetworkError && statusCode == 404

internal fun Throwable.isSameUserError(): Boolean = this is SameUserError