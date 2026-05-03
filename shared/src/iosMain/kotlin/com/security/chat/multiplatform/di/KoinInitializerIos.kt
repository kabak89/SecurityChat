package com.security.chat.multiplatform.di

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
}
