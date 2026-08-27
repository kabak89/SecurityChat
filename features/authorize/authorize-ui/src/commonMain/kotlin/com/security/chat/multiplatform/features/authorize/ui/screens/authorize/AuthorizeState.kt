package com.security.chat.multiplatform.features.authorize.ui.screens.authorize

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.features.authorize.ui.screens.authorize.entity.Connection
import com.security.chat.multiplatform.features.authorize.ui.screens.authorize.entity.Node
import kotlinx.collections.immutable.PersistentList

@Immutable
internal data class AuthorizeState(
    val currentTime: Long,
    val nodes: PersistentList<Node>,
    val connections: PersistentList<Connection>,
)
