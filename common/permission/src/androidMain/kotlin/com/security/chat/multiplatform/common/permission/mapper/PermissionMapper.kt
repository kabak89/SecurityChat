package com.security.chat.multiplatform.common.permission.mapper

import android.Manifest
import com.security.chat.multiplatform.common.permission.entity.Permission

internal fun Permission.toAndroid(): String {
    return when (this) {
        Permission.Notifications -> Manifest.permission.POST_NOTIFICATIONS
    }
}