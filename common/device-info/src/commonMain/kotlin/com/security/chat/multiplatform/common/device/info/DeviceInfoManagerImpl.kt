package com.security.chat.multiplatform.common.device.info

import com.security.chat.multiplatform.common.device.info.entity.Platform

internal expect class DeviceInfoManagerImpl() : DeviceInfoManager {

    override fun getPlatform(): Platform

    override fun getDeviceName(): String
}
