package com.security.chat.multiplatform.features.push.data.di

import com.security.chat.multiplatform.features.push.data.LocalChatNotifications
import com.security.chat.multiplatform.features.push.data.PushNotificationsManager
import com.security.chat.multiplatform.features.push.data.PushRepositoryImpl
import com.security.chat.multiplatform.features.push.data.registerLocalChatNotificationPresenter
import com.security.chat.multiplatform.features.push.data.storage.PushStorage
import com.security.chat.multiplatform.features.push.data.storage.PushStorageImpl
import com.security.chat.multiplatform.features.push.domain.PushRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val pushDataModule: Module =
    module {
        singleOf(::PushRepositoryImpl) bind PushRepository::class
        singleOf(::PushStorageImpl) bind PushStorage::class
        singleOf(::PushNotificationsManager)
        singleOf(::LocalChatNotifications)
        registerLocalChatNotificationPresenter()
    }
