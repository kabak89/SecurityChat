package com.security.chat.multiplatform.features.chat_info.ui.screens.addmember.mapper

import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.entity.UiError
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.features.chat_info.domain.entity.errors.UserAlreadyInChat
import com.security.chat.multiplatform.features.chat_info.domain.entity.errors.UserAlreadyInListForAdding
import securitychat.common.localization.generated.resources.add_member_user_already_in_chat_error_description
import securitychat.common.localization.generated.resources.add_member_user_already_in_chat_error_title
import securitychat.common.localization.generated.resources.add_member_user_already_in_list_error_description
import securitychat.common.localization.generated.resources.add_member_user_already_in_list_error_title

internal fun createAddMemberErrorMapper(error: Throwable): UiError? {
    return when {
        error.isUserAlreadyInChatError() -> {
            UiError(
                title = resPrintableText(StringRes.add_member_user_already_in_chat_error_title),
                description = resPrintableText(StringRes.add_member_user_already_in_chat_error_description),
                image = null,
                cause = error,
            )
        }

        error.isUserAlreadyInListError() -> {
            UiError(
                title = resPrintableText(StringRes.add_member_user_already_in_list_error_title),
                description = resPrintableText(StringRes.add_member_user_already_in_list_error_description),
                image = null,
                cause = error,
            )
        }

        else -> {
            null
        }
    }
}

internal fun Throwable.isUserAlreadyInChatError(): Boolean = this is UserAlreadyInChat

internal fun Throwable.isUserAlreadyInListError(): Boolean = this is UserAlreadyInListForAdding