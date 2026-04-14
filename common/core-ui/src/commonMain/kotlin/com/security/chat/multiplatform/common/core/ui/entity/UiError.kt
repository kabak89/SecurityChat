package com.security.chat.multiplatform.common.core.ui.entity

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource

@Immutable
public data class UiError(
    val title: PrintableText,
    val description: PrintableText?,
    val image: DrawableResource?,
    val cause: Throwable,
)
