package com.security.chat.multiplatform.common.device.info

import com.security.chat.multiplatform.common.device.info.entity.Platform

internal actual class DeviceInfoManagerImpl : DeviceInfoManager {

    override fun getPlatform(): Platform = Platform.Desktop
}
