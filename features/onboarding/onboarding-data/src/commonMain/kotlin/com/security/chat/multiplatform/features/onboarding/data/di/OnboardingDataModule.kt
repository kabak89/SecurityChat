package com.security.chat.multiplatform.features.onboarding.data.di

import com.security.chat.multiplatform.features.onboarding.data.OnboardingRepoImpl
import com.security.chat.multiplatform.features.onboarding.domain.repo.OnboardingRepo
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val onboardingDataModule: Module =
    module {
        singleOf(::OnboardingRepoImpl) bind OnboardingRepo::class
    }
