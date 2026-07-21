package com.security.chat.multiplatform.features.chat.domain.entity

import android.content.ContentResolver
import android.net.Uri
import com.security.chat.multiplatform.common.core.files.FileSource

public actual class PickedImage(
    uri: Uri,
    contentResolver: ContentResolver,
) {

    internal val fileSource = FileSource(uri, contentResolver)
}

public actual fun PickedImage.toFileSource(): FileSource {
    return fileSource
}
