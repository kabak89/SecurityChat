package com.security.chat.multiplatform.features.profile.data.di

import com.security.chat.multiplatform.features.profile.data.ProfileRepoImpl
import com.security.chat.multiplatform.features.profile.domain.repo.ProfileRepo
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val profileDataModule: Module =
    module {
        singleOf(::ProfileRepoImpl) bind ProfileRepo::class
    }
