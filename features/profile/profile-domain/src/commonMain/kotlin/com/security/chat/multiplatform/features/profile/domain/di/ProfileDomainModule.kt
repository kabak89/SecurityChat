package com.security.chat.multiplatform.features.profile.domain.di

import com.security.chat.multiplatform.features.profile.domain.ProfileModel
import com.security.chat.multiplatform.features.profile.domain.ProfileModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val profileDomainModule: Module =
    module {
        singleOf(::ProfileModelImpl) bind ProfileModel::class
    }
