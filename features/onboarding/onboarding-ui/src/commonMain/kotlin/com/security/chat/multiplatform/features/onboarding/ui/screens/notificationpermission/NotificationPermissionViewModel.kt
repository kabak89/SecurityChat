package com.security.chat.multiplatform.features.onboarding.ui.screens.notificationpermission

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.onboarding.domain.OnboardingModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.kode.remo.successResults

internal class NotificationPermissionViewModel(
    private val onboardingModel: OnboardingModel,
) : BaseViewModel<NotificationPermissionState, NotificationPermissionEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        onboardingModel.finishOnboarding.jobFlow.successResults()
            .onEach {
                sendEvent(NotificationPermissionEvent.Finished)
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): NotificationPermissionState {
        return NotificationPermissionState()
    }

    fun onFinishOnboardingClicked() {
        onboardingModel.finishOnboarding.start()
    }
}
