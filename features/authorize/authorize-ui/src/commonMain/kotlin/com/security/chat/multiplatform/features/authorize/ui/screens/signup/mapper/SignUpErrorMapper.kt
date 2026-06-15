package com.security.chat.multiplatform.features.authorize.ui.screens.signup.mapper

import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.entity.UiError
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import securitychat.common.localization.generated.resources.sign_up_username_already_exists_error_description
import securitychat.common.localization.generated.resources.sign_up_username_already_exists_error_title

internal fun signUpErrorMapper(error: Throwable): UiError? {
    return when {
        error.isUsernameAlreadyExists() -> {
            UiError(
                title = resPrintableText(StringRes.sign_up_username_already_exists_error_title),
                description =
                    resPrintableText(StringRes.sign_up_username_already_exists_error_description),
                image = null,
                cause = error,
            )
        }

        else -> {
            null
        }
    }
}

internal fun Throwable.isUsernameAlreadyExists(): Boolean {
    return this is NetworkError && this.statusCode == 403
}