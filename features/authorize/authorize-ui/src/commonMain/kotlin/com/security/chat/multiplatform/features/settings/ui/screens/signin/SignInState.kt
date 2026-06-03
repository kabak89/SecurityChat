package com.security.chat.multiplatform.features.settings.ui.screens.signin

import com.security.chat.multiplatform.features.settings.ui.screens.signin.entity.AlertDialogDescriptor

internal data class SignInState(
    val username: String,
    val password: String,
    val isLoading: Boolean,
    val isSignInEnabled: Boolean,
    val alertDialogDescriptor: AlertDialogDescriptor?,
)
