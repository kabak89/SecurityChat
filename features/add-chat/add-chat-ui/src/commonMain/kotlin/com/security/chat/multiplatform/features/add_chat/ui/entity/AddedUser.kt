package com.security.chat.multiplatform.features.add_chat.ui.entity

import androidx.compose.runtime.Immutable

@Immutable
internal data class AddedUser(
    val id: String,
    val username: String,
)
