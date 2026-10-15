package com.security.chat.multiplatform.features.call.ui.screens.call.mapper

import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.entity.UiError
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.features.call.domain.entity.errors.CallMediaError
import com.security.chat.multiplatform.features.call.domain.entity.errors.CallSignalingError
import securitychat.common.localization.generated.resources.call_connection_error_description
import securitychat.common.localization.generated.resources.call_error_description
import securitychat.common.localization.generated.resources.call_error_title
import securitychat.common.localization.generated.resources.call_unsupported_description

internal fun callErrorMapper(error: Throwable): UiError? {
    val description = when (error) {
        is CallSignalingError -> StringRes.call_connection_error_description
        is CallMediaError -> StringRes.call_error_description
        is NotImplementedError -> StringRes.call_unsupported_description
        else -> return null
    }
    return UiError(
        title = resPrintableText(StringRes.call_error_title),
        description = resPrintableText(description),
        image = null,
        cause = error,
    )
}
