package com.security.chat.multiplatform.features.profile.ui.screens.main

import com.security.chat.multiplatform.common.core.ui.entity.PrintableText

internal sealed interface ProfileMainEvent {
    data class Toast(val text: PrintableText) : ProfileMainEvent
}