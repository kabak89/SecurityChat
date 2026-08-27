package com.security.chat.multiplatform.features.authorize.ui.screens.authorize.entity

import androidx.compose.runtime.Immutable

@Immutable
internal data class Connection(
    val id: String,
    val start: Node,
    val end: Node,
    val lifeTime: Long,
    val messageProgress: MessageProgress?,
) {
    enum class Direction {
        StartToEnd,
        EndToStart,
    }

    @Immutable
    data class MessageProgress(
        val direction: Direction,
        val progress: Float,
    )
}
