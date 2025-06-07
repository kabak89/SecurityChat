package com.security.chat.multiplatform.common.core.db.di

import com.security.chat.multiplatform.common.core.db.NotSecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.db.NotSecuredDatabaseDriverFactoryIos
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactoryIos
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val coreDbModule: Module =
    module {
        singleOf(::SecuredDatabaseDriverFactoryIos) bind SecuredDatabaseDriverFactory::class
        singleOf(::NotSecuredDatabaseDriverFactoryIos) bind NotSecuredDatabaseDriverFactory::class
    }