package com.security.chat.multiplatform.features.profile.ui.screens.deleteprofile.entity

import androidx.compose.runtime.Immutable

@Immutable
internal data class ObstacleSpec(
    val width: Int,
    val height: Int,
    val xCoordinate: Float,
    val yCoordinate: Float,
)
