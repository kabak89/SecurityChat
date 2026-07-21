package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.security.chat.multiplatform.features.chat.domain.entity.PickedImage

@Composable
internal actual fun rememberPhotoPickerLauncher(
    onPhotoPicked: (PickedImage) -> Unit,
): PhotoPickerLauncher {
    val currentOnPhotoPicked = rememberUpdatedState(onPhotoPicked)
    val contentResolver = LocalContext.current.contentResolver

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                currentOnPhotoPicked.value(
                    PickedImage(
                        uri = uri,
                        contentResolver = contentResolver,
                    ),
                )
            }
        },
    )

    return remember(pickerLauncher) {
        PhotoPickerLauncher {
            pickerLauncher.launch(
                PickVisualMediaRequest(
                    mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
                ),
            )
        }
    }
}
