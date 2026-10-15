package com.security.chat.multiplatform.features.onboarding.ui.screens.welcome

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.permission.PermissionsManager
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.features.onboarding.domain.OnboardingModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.kode.remo.successResults

internal class WelcomeViewModel(
    private val permissionsManager: PermissionsManager,
    private val onboardingModel: OnboardingModel,
) : BaseViewModel<WelcomeState, WelcomeEvent>() {

    override fun createInitialState(): WelcomeState = WelcomeState

    override fun onPostStart() {
        super.onPostStart()

        onboardingModel.finishOnboarding.jobFlow.successResults()
            .onEach { sendEvent(WelcomeEvent.Finished) }
            .launchIn(viewModelScope)
    }

    fun onContinueClicked() {
        when (permissionsManager.isPermissionAllowed(Permission.Notifications)) {
            AllowanceResult.Allowed -> onboardingModel.finishOnboarding.start()
            AllowanceResult.Restricted -> sendEvent(WelcomeEvent.RequestNotificationPermission)
        }
    }
}
