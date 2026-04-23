package com.security.chat.multiplatform.features.chat.ui.screens.personalchat.entity

import androidx.compose.runtime.Immutable

@Immutable
internal data class InterlocutorUM(
    val name: String,
    val isOnline: Boolean,
)
