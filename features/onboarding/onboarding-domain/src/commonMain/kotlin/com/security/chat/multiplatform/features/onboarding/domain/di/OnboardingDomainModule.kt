package com.security.chat.multiplatform.features.onboarding.domain.di

import com.security.chat.multiplatform.features.onboarding.domain.OnboardingModel
import com.security.chat.multiplatform.features.onboarding.domain.OnboardingModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val onboardingDomainModule: Module =
    module {
        singleOf(::OnboardingModelImpl) bind OnboardingModel::class
    }
