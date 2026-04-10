package com.security.chat.multiplatform.features.settings.ui.screens.main

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.settings.domain.SettingsModel
import com.security.chat.multiplatform.features.settings.ui.screens.main.entity.DialogData
import com.security.chat.multiplatform.features.settings.ui.screens.main.entity.SettingItem
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.kode.remo.successResults

internal class SettingsMainViewModel(
    private val settingsModel: SettingsModel,
) : BaseViewModel<SettingsMainState, SettingsMainEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        settingsModel.logout.jobFlow.asLceState()
            .onEach { state ->
                val isInProgress = state.isLoading
                updateState { it.copy(requestInProgress = isInProgress) }
            }
            .launchIn(viewModelScope)

        settingsModel.logout.jobFlow.successResults()
            .onEach {
                sendEvent(SettingsMainEvent.UserLogOuted)
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): SettingsMainState {
        return SettingsMainState(
            items = listOf(
                SettingItem.Profile,
                SettingItem.Theme,
                SettingItem.Logout,
            ),
            dialogData = null,
            requestInProgress = false,
        )
    }

    fun onItemClicked(item: SettingItem) {
        when (item) {
            is SettingItem.Logout -> {
                updateState {
                    it.copy(
                        dialogData = DialogData(
                            title = "Log out",
                            message = "Are you sure?",
                            positiveButtonTitle = "Ok",
                            negativeButtonTitle = "Cancel",
                            positiveButtonAction = DialogData.ButtonAction.Ok,
                            negativeButtonAction = DialogData.ButtonAction.Cancel,
                        ),
                    )
                }
            }

            is SettingItem.Theme -> sendEvent(SettingsMainEvent.GoToTheme)
            is SettingItem.Profile -> sendEvent(SettingsMainEvent.GoToProfile)
        }
    }

    fun onDismissDialogClicked() {
        updateState { it.copy(dialogData = null) }
    }

    fun onDialogActionClicked(action: DialogData.ButtonAction) {
        when (action) {
            DialogData.ButtonAction.Cancel -> {
                updateState { it.copy(dialogData = null) }
            }

            DialogData.ButtonAction.Ok -> {
                updateState { it.copy(dialogData = null) }
                settingsModel.logout.startOnSubscribe()
            }
        }
    }

}