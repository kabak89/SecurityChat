package com.security.chat.multiplatform.features.onboarding.ui.screens.notificationpermission

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.features.onboarding.domain.OnboardingModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.kode.remo.successResults
import securitychat.common.localization.generated.resources.common_ok
import securitychat.common.localization.generated.resources.notification_permission_restricted_permission_dialog_message
import securitychat.common.localization.generated.resources.notification_permission_restricted_permission_dialog_title

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
        return NotificationPermissionState(
            alertDialogDescriptor = null,
        )
    }

    fun onFinishOnboardingClicked() {
        onboardingModel.finishOnboarding.start()
    }

    fun showPermissionRestrictDialog() {
        val alertDialogDescriptor = AlertDialogDescriptor(
            content = AlertDialogContent(
                title = resPrintableText(
                    StringRes.notification_permission_restricted_permission_dialog_title,
                ),
                message = resPrintableText(
                    StringRes.notification_permission_restricted_permission_dialog_message,
                ),
                positiveButtonText = resPrintableText(StringRes.common_ok),
            ),
            dismissAction = { onboardingModel.finishOnboarding.start() },
            positiveAction = { onboardingModel.finishOnboarding.start() },
        )
        updateState { it.copy(alertDialogDescriptor = alertDialogDescriptor) }
    }
}
