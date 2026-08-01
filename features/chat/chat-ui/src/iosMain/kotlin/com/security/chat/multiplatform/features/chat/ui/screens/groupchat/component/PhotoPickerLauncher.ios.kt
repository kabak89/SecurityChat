package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.security.chat.multiplatform.features.chat.domain.entity.PickedImage
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerConfigurationAssetRepresentationModeCompatible
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSObject

@Composable
internal actual fun rememberPhotoPickerLauncher(
    onImagePicked: (PickedImage) -> Unit,
): PhotoPickerLauncher {
    val currentOnImagePicked = rememberUpdatedState(onImagePicked)

    return remember {
        IosPhotoPickerLauncher(
            onPhotoPicked = { photo ->
                currentOnImagePicked.value(photo)
            },
        )
    }
}

private class IosPhotoPickerLauncher(
    private val onPhotoPicked: (PickedImage) -> Unit,
) : PhotoPickerLauncher {

    private var delegate: PhotoPickerDelegate? = null

    override fun launch() {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: return
        val photoPickerDelegate = PhotoPickerDelegate(onPhotoPicked)
        delegate = photoPickerDelegate
        val photoPicker = PHPickerViewController(
            configuration = PHPickerConfiguration().apply {
                filter = PHPickerFilter.imagesFilter
                selectionLimit = 1
                preferredAssetRepresentationMode =
                    PHPickerConfigurationAssetRepresentationModeCompatible
            },
        ).apply {
            delegate = photoPickerDelegate
        }

        rootViewController.presentViewController(
            viewControllerToPresent = photoPicker,
            animated = true,
            completion = null,
        )
    }

    private inner class PhotoPickerDelegate(
        private val onPhotoPicked: (PickedImage) -> Unit,
    ) : NSObject(), PHPickerViewControllerDelegateProtocol {

        override fun picker(
            picker: PHPickerViewController,
            didFinishPicking: List<*>,
        ) {
            picker.dismissViewControllerAnimated(
                flag = true,
                completion = null,
            )
            delegate = null

            val selectedPhoto = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
            onPhotoPicked(PickedImage(selectedPhoto.itemProvider))
        }
    }
}
