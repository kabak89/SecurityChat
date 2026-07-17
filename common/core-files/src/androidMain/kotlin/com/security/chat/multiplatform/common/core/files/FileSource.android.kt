package com.security.chat.multiplatform.common.core.files

import android.content.ContentResolver
import android.net.Uri

public actual class FileSource(
    internal val uri: Uri,
    internal val contentResolver: ContentResolver,
)
