package com.security.chat.multiplatform.features.settings.ui.screens.signin.mapper

import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.entity.UiError
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import securitychat.common.localization.generated.resources.sign_in_user_not_found_error_description
import securitychat.common.localization.generated.resources.sign_in_user_not_found_error_title
import securitychat.common.localization.generated.resources.sign_in_wrong_password_error_description
import securitychat.common.localization.generated.resources.sign_in_wrong_password_error_title

internal fun signInErrorMapper(error: Throwable): UiError? {
    return when (error) {
        is NetworkError if error.statusCode == 404 -> {
            UiError(
                title = resPrintableText(StringRes.sign_in_user_not_found_error_title),
                description = resPrintableText(StringRes.sign_in_user_not_found_error_description),
                image = null,
                cause = error,
            )
        }

        is NetworkError if error.statusCode == 403 -> {
            UiError(
                title = resPrintableText(StringRes.sign_in_wrong_password_error_title),
                description = resPrintableText(StringRes.sign_in_wrong_password_error_description),
                image = null,
                cause = error,
            )
        }

        else -> {
            null
        }
    }
}