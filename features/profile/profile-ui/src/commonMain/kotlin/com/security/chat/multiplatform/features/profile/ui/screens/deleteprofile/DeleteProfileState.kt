package com.security.chat.multiplatform.features.profile.ui.screens.deleteprofile

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor

@Immutable
internal data class DeleteProfileState(
    val showLoading: Boolean,
    val alertDialogDescriptor: AlertDialogDescriptor?,
)
