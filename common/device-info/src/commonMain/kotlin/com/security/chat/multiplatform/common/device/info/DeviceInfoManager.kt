package com.security.chat.multiplatform.common.device.info

import com.security.chat.multiplatform.common.device.info.entity.Platform

public interface DeviceInfoManager {

    public fun getPlatform(): Platform

    public fun getDeviceName(): String
}
