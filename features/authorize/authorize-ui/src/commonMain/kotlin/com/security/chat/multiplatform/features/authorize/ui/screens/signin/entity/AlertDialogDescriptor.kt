package com.security.chat.multiplatform.features.authorize.ui.screens.signin.entity

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent

@Immutable
internal data class AlertDialogDescriptor(
    val content: AlertDialogContent,
    val dismissAction: (() -> Unit),
    val positiveAction: (() -> Unit)? = null,
    val negativeAction: (() -> Unit)? = null,
)
