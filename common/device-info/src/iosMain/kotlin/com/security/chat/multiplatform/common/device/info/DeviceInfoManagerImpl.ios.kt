package com.security.chat.multiplatform.common.device.info

import com.security.chat.multiplatform.common.device.info.entity.Platform
import platform.UIKit.UIDevice

internal actual class DeviceInfoManagerImpl : DeviceInfoManager {

    actual override fun getPlatform(): Platform = Platform.IOS

    actual override fun getDeviceName(): String = UIDevice.currentDevice.name
}
