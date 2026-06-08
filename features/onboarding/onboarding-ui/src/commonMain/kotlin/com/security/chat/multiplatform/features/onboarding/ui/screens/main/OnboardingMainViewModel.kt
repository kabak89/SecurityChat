package com.security.chat.multiplatform.features.onboarding.ui.screens.main

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.onboarding.domain.OnboardingModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.kode.remo.successResults

internal class OnboardingMainViewModel(
    private val onboardingModel: OnboardingModel,
) : BaseViewModel<OnboardingMainState, OnboardingMainEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        onboardingModel.finishOnboarding.jobFlow.successResults()
            .onEach {
                sendEvent(OnboardingMainEvent.Finished)
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): OnboardingMainState {
        return OnboardingMainState()
    }

    fun onFinishOnboardingClicked() {
        onboardingModel.finishOnboarding.start()
    }
}
