package com.security.chat.multiplatform.di

import com.security.chat.multiplatform.applifecycle.AppLifecycleChanger
import com.security.chat.multiplatform.applifecycle.AppLifecycleImpl
import com.security.chat.multiplatform.common.app.lifecycle.AppLifecycle
import com.security.chat.multiplatform.features.chat.data.common.ChatEventsPublisher
import com.security.chat.multiplatform.features.chat.data.common.OutgoingMessageScheduler
import com.security.chat.multiplatform.features.settings.data.common.SettingsCleanupHook
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.binds
import org.koin.dsl.module

internal val sharedModule: Module =
    module {
        singleOf(::AppLifecycleImpl) binds arrayOf(
            AppLifecycle::class,
            AppLifecycleChanger::class,
        )
        single<SettingsCleanupHook> {
            val chatEventsPublisher: ChatEventsPublisher = get()
            val outgoingMessageScheduler: OutgoingMessageScheduler = get()
            SettingsCleanupHook {
                outgoingMessageScheduler.cancel()
                chatEventsPublisher.stop()
            }
        }
    }
