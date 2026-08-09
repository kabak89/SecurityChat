package com.security.chat.multiplatform.common.device.info

import android.os.Build
import com.security.chat.multiplatform.common.device.info.entity.Platform

internal actual class DeviceInfoManagerImpl : DeviceInfoManager {

    actual override fun getPlatform(): Platform = Platform.Android

    actual override fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
