package com.security.chat.multiplatform.di

import com.security.chat.multiplatform.features.push.data.di.pushBackgroundWorkModule
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.KoinAppDeclaration

internal actual fun initKoin(appDeclaration: KoinAppDeclaration) {
    startKoin {
        androidLogger(level = Level.NONE)
        appDeclaration()
        modules(commonAppDiModules + pushBackgroundWorkModule)
    }
}
