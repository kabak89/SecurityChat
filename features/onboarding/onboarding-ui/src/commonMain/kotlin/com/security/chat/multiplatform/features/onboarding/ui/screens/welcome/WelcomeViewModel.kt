package com.security.chat.multiplatform.features.onboarding.ui.screens.welcome

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.permission.PermissionsManager
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.features.onboarding.domain.OnboardingModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.kode.remo.successResults

internal class WelcomeViewModel(
    private val permissionsManager: PermissionsManager,
    private val onboardingModel: OnboardingModel,
) : BaseViewModel<WelcomeState, WelcomeEvent>() {

    private var permissionCheck: Job? = null

    override fun createInitialState(): WelcomeState = WelcomeState

    override fun onPostStart() {
        super.onPostStart()

        onboardingModel.finishOnboarding.jobFlow.successResults()
            .onEach { sendEvent(WelcomeEvent.Finished) }
            .launchIn(viewModelScope)
    }

    fun onContinueClicked() {
        if (permissionCheck?.isActive == true) return
        permissionCheck = viewModelScope.launch {
            if (permissionsManager.checkPermission(Permission.Notifications) == AllowanceResult.Restricted ||
                permissionsManager.checkPermission(Permission.Bluetooth) == AllowanceResult.Restricted ||
                needsBluetoothBackgroundLocation()
            ) {
                sendEvent(WelcomeEvent.RequestPermissions)
            } else {
                onboardingModel.finishOnboarding.start()
            }
        }
    }

    private suspend fun needsBluetoothBackgroundLocation(): Boolean {
        return permissionsManager.bluetoothScanRequiresBackgroundLocation() &&
                permissionsManager.checkPermission(Permission.BluetoothBackgroundLocation) == AllowanceResult.Restricted
    }
}
