package com.security.chat.multiplatform.features.authorize.data.di

import com.security.chat.multiplatform.features.authorize.component.SCOPE_ID_SIGN_IN
import com.security.chat.multiplatform.features.authorize.data.repoimpl.SignInRepoImpl
import com.security.chat.multiplatform.features.authorize.domain.repo.SignInRepo
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val authorizeDataModule: Module =
    module {
        scope(named(SCOPE_ID_SIGN_IN)) {
            scopedOf(::SignInRepoImpl) bind SignInRepo::class
        }
    }