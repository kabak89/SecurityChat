package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.security.chat.multiplatform.features.chat.domain.entity.PickedPhoto
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

@Composable
internal actual fun rememberPhotoPickerLauncher(
    onPhotoPicked: (PickedPhoto) -> Unit,
): PhotoPickerLauncher {
    val currentOnPhotoPicked = rememberUpdatedState(onPhotoPicked)

    return remember {
        PhotoPickerLauncher {
            val dialog = FileDialog(
                null as Frame?,
                "Choose a photo",
                FileDialog.LOAD,
            ).apply {
                filenameFilter = FilenameFilter { _, name ->
                    name.endsWith(".jpg", ignoreCase = true) ||
                            name.endsWith(".jpeg", ignoreCase = true) ||
                            name.endsWith(".png", ignoreCase = true) ||
                            name.endsWith(".webp", ignoreCase = true)
                }
                isVisible = true
            }

            val fileName = dialog.file ?: return@PhotoPickerLauncher
            val directory = dialog.directory ?: return@PhotoPickerLauncher
            currentOnPhotoPicked.value(PickedPhoto(File(directory, fileName)))
        }
    }
}
