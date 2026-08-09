package com.security.chat.multiplatform.common.device.info

import com.security.chat.multiplatform.common.device.info.entity.Platform

internal actual class DeviceInfoManagerImpl : DeviceInfoManager {

    actual override fun getPlatform(): Platform = Platform.Desktop

    actual override fun getDeviceName(): String {
        val osName = System.getProperty("os.name")
        return "Desktop $osName"
    }
}
