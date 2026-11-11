package com.security.chat.multiplatform.di

import com.security.chat.multiplatform.common.background.work.registerIosBackgroundWork
import com.security.chat.multiplatform.features.mesh.data.startMeshBluetooth
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

internal actual fun initKoin(appDeclaration: KoinAppDeclaration) {
    startKoin {
        appDeclaration()
        modules(commonAppDiModules)
    }
}

@Suppress("unused")
public fun doInitKoin() {
    initDI()
    registerIosBackgroundWork()
    startMeshModel()
    startMeshBluetooth()
}
