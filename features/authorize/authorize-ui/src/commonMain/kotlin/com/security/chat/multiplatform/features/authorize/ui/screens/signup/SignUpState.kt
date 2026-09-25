package com.security.chat.multiplatform.features.authorize.ui.screens.signup

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor

@Immutable
internal data class SignUpState(
    val username: String,
    val isLoading: Boolean,
    val nextButtonEnabled: Boolean,
    val isOnboardingPassed: Boolean,
    val alertDialogDescriptor: AlertDialogDescriptor?,
)
