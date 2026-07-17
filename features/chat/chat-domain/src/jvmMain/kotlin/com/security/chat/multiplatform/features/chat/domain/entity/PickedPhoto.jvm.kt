package com.security.chat.multiplatform.features.chat.domain.entity

import com.security.chat.multiplatform.common.core.files.FileSource
import java.io.File

public actual class PickedPhoto(
    file: File,
) {

    internal val fileSource = FileSource(file)
}

public actual fun PickedPhoto.toFileSource(): FileSource {
    return fileSource
}
