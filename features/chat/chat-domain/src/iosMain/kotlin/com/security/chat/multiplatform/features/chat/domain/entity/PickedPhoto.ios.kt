package com.security.chat.multiplatform.features.chat.domain.entity

import com.security.chat.multiplatform.common.core.files.FileSource
import platform.Foundation.NSItemProvider

public actual class PickedPhoto(
    itemProvider: NSItemProvider,
) {

    internal val fileSource = FileSource(itemProvider)
}

public actual fun PickedPhoto.toFileSource(): FileSource {
    return fileSource
}
