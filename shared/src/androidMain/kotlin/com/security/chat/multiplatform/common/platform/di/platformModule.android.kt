package com.security.chat.multiplatform.common.platform.di

import android.content.Context
import com.security.chat.multiplatform.common.platform.ContextHolder
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

internal actual val platformModule: Module =
    module {
        single {
            ContextHolder.getContext()
        } bind Context::class
    }