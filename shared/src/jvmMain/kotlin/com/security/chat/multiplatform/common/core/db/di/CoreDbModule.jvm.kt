package com.security.chat.multiplatform.common.core.db.di

import com.security.chat.multiplatform.common.core.db.NotSecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.db.NotSecuredDatabaseDriverFactoryDesktop
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactoryDesktop
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val coreDbModule: Module =
    module {

        singleOf(::SecuredDatabaseDriverFactoryDesktop) bind SecuredDatabaseDriverFactory::class

        singleOf(::NotSecuredDatabaseDriverFactoryDesktop) bind
                NotSecuredDatabaseDriverFactory::class
    }