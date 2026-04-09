package com.security.chat.multiplatform.features.authorize.data.di

import com.security.chat.multiplatform.features.authorize.data.repoimpl.SignInRepoImpl
import com.security.chat.multiplatform.features.authorize.data.repoimpl.SignUpRepoImpl
import com.security.chat.multiplatform.features.authorize.domain.repo.SignInRepo
import com.security.chat.multiplatform.features.authorize.domain.repo.SignUpRepo
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val authorizeDataModule: Module =
    module {
        singleOf(::SignInRepoImpl) bind SignInRepo::class
        singleOf(::SignUpRepoImpl) bind SignUpRepo::class
    }