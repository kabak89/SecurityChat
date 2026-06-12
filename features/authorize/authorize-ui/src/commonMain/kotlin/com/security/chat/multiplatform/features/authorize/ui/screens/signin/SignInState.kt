package com.security.chat.multiplatform.features.authorize.ui.screens.signin

import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor

internal data class SignInState(
    val privateKey: String,
    val isLoading: Boolean,
    val isSignInEnabled: Boolean,
    val alertDialogDescriptor: AlertDialogDescriptor?,
    val isOnboardingPassed: Boolean,
)
