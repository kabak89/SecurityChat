package com.security.chat.multiplatform.common.core.db.di

import com.security.chat.multiplatform.common.core.db.NotSecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.db.NotSecuredDatabaseDriverFactoryAndroid
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactoryAndroid
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public actual val coreDbModule: Module =
    module {
        singleOf(::SecuredDatabaseDriverFactoryAndroid) bind SecuredDatabaseDriverFactory::class
        singleOf(::NotSecuredDatabaseDriverFactoryAndroid) bind
                NotSecuredDatabaseDriverFactory::class
    }
