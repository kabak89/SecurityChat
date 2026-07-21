package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.runtime.Composable
import com.security.chat.multiplatform.features.chat.domain.entity.PickedImage

internal fun interface PhotoPickerLauncher {

    fun launch()
}

@Composable
internal expect fun rememberPhotoPickerLauncher(
    onPhotoPicked: (PickedImage) -> Unit,
): PhotoPickerLauncher
