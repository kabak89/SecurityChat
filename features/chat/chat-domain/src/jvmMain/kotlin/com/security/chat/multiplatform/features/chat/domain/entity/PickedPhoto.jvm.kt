package com.security.chat.multiplatform.features.chat.domain.entity

import com.security.chat.multiplatform.common.core.files.FileSource
import java.io.File

public actual class PickedImage(
    file: File,
) {

    internal val fileSource = FileSource(file)
}

public actual fun PickedImage.toFileSource(): FileSource {
    return fileSource
}
