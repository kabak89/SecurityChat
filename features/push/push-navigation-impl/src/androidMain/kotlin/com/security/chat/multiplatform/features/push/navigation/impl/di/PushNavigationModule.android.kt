package com.security.chat.multiplatform.features.push.navigation.impl.di

import com.security.chat.multiplatform.features.push.navigation.api.IntentBuilder
import com.security.chat.multiplatform.features.push.navigation.impl.IntentBuilderImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public actual val pushNavigationModule: Module =
    module {
        singleOf(::IntentBuilderImpl) bind IntentBuilder::class
    }