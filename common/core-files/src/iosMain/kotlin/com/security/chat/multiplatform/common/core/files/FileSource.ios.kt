package com.security.chat.multiplatform.common.core.files

import platform.Foundation.NSItemProvider

public actual class FileSource(
    internal val itemProvider: NSItemProvider,
)
