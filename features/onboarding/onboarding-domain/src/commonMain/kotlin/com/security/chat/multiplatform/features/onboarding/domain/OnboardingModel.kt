package com.security.chat.multiplatform.features.onboarding.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.onboarding.domain.repo.OnboardingRepo
import ru.kode.remo.Task0

public interface OnboardingModel : ScopedModel {

    public val finishOnboarding: Task0<Unit>
}

internal class OnboardingModelImpl(
    private val onboardingRepo: OnboardingRepo,
    dispatcherProvider: DispatcherProviderInterface,
) : OnboardingModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    override val finishOnboarding: Task0<Unit> =
        task { ->
            onboardingRepo.finishOnboarding()
        }
}
