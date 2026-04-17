package com.security.chat.multiplatform.features.profile.ui.screens.main.entity

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent

@Immutable
internal data class DialogContent(
    val errorDialogContent: AlertDialogContent,
    val dismissAction: (() -> Unit),
    val positiveAction: (() -> Unit)?,
    val negativeAction: (() -> Unit)?,
)
