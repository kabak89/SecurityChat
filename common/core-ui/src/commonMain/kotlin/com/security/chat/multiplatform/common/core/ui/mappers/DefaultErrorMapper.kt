package com.security.chat.multiplatform.common.core.ui.mappers

import com.security.chat.multiplatform.common.core.error.ConnectionError
import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.common.core.error.UnknownError
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.entity.UiError
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import securitychat.common.localization.generated.resources.error_connection_description
import securitychat.common.localization.generated.resources.error_connection_title
import securitychat.common.localization.generated.resources.error_server_internal_error_description
import securitychat.common.localization.generated.resources.error_server_internal_error_title
import securitychat.common.localization.generated.resources.error_server_unknown_error_description
import securitychat.common.localization.generated.resources.error_server_unknown_error_title
import securitychat.common.localization.generated.resources.error_unknown_description
import securitychat.common.localization.generated.resources.error_unknown_title

public object DefaultErrorMapper {

    public fun mapError(error: Throwable): UiError {
        return when (error) {
            is NetworkError -> buildNetworkError(error)
            is ConnectionError -> buildConnectionError(error)
            is UnknownError -> buildUnknownError(error)
            else -> buildUnknownError(error)
        }
    }
}

private fun buildNetworkError(error: NetworkError): UiError {
    return when (error.statusCode) {
        500 -> {
            UiError(
                title = resPrintableText(StringRes.error_server_internal_error_title),
                description = resPrintableText(StringRes.error_server_internal_error_description),
                cause = error,
                image = null,
            )
        }

        else -> {
            UiError(
                title = resPrintableText(StringRes.error_server_unknown_error_title),
                description = resPrintableText(StringRes.error_server_unknown_error_description),
                cause = error,
                image = null,
            )
        }
    }
}

private fun buildConnectionError(error: Throwable): UiError {
    return UiError(
        title = resPrintableText(StringRes.error_connection_title),
        description = resPrintableText(StringRes.error_connection_description),
        cause = error,
        image = null,
    )
}

private fun buildUnknownError(error: Throwable): UiError {
    return UiError(
        title = resPrintableText(StringRes.error_unknown_title),
        description = resPrintableText(StringRes.error_unknown_description),
        cause = error,
        image = null,
    )
}