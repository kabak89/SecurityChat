package com.security.chat.multiplatform.common.core.files.di

import com.security.chat.multiplatform.common.core.files.FileManager
import com.security.chat.multiplatform.common.core.files.FileManagerAndroid
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public actual val coreFilesModule: Module =
    module {
        singleOf(::FileManagerAndroid) bind FileManager::class
    }
