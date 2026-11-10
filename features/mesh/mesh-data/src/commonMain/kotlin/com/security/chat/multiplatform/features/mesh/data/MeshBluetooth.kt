package com.security.chat.multiplatform.features.mesh.data

import org.koin.mp.KoinPlatform

public fun startMeshBluetooth() {
    KoinPlatform.getKoin().get<BluetoothManager>().start()
}
