package com.security.chat.multiplatform.common.ui.kit.components.alertdialog

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.core.ui.entity.PrintableText

@Immutable
public data class AlertDialogContent(
    val title: PrintableText,
    val message: PrintableText? = null,
    val positiveButtonText: PrintableText? = null,
    val negativeButtonText: PrintableText? = null,
)