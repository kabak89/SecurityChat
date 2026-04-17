package com.security.chat.multiplatform.features.profile.ui.screens.main.mapper

import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.entity.UiError
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import securitychat.common.localization.generated.resources.profile_update_conflict_error_description
import securitychat.common.localization.generated.resources.profile_update_conflict_error_title

internal fun updateProfileErrorMapper(error: Throwable): UiError? {
    return if (error is NetworkError && error.statusCode == 409) {
        UiError(
            title = resPrintableText(StringRes.profile_update_conflict_error_title),
            description = resPrintableText(StringRes.profile_update_conflict_error_description),
            image = null,
            cause = error,
        )
    } else {
        null
    }
}