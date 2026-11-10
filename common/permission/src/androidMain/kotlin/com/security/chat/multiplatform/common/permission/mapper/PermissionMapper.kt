package com.security.chat.multiplatform.common.permission.mapper

import android.Manifest
import android.os.Build
import com.security.chat.multiplatform.common.permission.bluetoothScanRequiresLocation
import com.security.chat.multiplatform.common.permission.entity.Permission

internal fun Permission.toAndroid(): Array<String> {
    return when (this) {
        Permission.Notifications -> arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        Permission.RecordAudio -> arrayOf(Manifest.permission.RECORD_AUDIO)
        Permission.LocalNetwork -> arrayOf(Manifest.permission.ACCESS_LOCAL_NETWORK)
        Permission.Bluetooth -> if (bluetoothScanRequiresLocation()) {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
            )
        }

        Permission.BluetoothBackgroundLocation ->
            if (Build.VERSION.SDK_INT in Build.VERSION_CODES.Q..Build.VERSION_CODES.R) {
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                emptyArray()
            }
    }
}
