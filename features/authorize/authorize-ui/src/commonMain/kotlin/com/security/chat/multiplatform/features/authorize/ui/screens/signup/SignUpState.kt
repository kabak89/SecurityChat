package com.security.chat.multiplatform.features.authorize.ui.screens.signup

import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor

internal data class SignUpState(
    val username: String,
    val isLoading: Boolean,
    val nextButtonEnabled: Boolean,
    val isOnboardingPassed: Boolean,
    val alertDialogDescriptor: AlertDialogDescriptor?,
)
