package com.security.chat.multiplatform.common.ui.kit.components.alertdialog

import androidx.compose.runtime.Immutable

@Immutable
public data class AlertDialogDescriptor(
    val content: AlertDialogContent,
    val dismissAction: (() -> Unit),
    val positiveAction: (() -> Unit)? = null,
    val negativeAction: (() -> Unit)? = null,
)