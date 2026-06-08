package com.security.chat.multiplatform.features.onboarding.data

import com.security.chat.multiplatform.features.onboarding.domain.repo.OnboardingRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage

internal class OnboardingRepoImpl(
    private val userStorage: UserStorage,
) : OnboardingRepo {

    override suspend fun finishOnboarding() {
        userStorage.saveOnboardingPassed()
    }
}
