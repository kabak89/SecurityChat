package com.security.chat.multiplatform.features.settings.ui.screens.main

import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.settings.ui.screens.main.entity.DialogData
import com.security.chat.multiplatform.features.settings.ui.screens.main.entity.SettingItem

internal class SettingsMainViewModel : BaseViewModel<SettingsMainState, SettingsMainEvent>() {

    override fun createInitialState(): SettingsMainState {
        return SettingsMainState(
            items = listOf(
                SettingItem.Logout(
                    title = "Log out",
                ),
            ),
            dialogData = null,
        )
    }

    fun onItemClicked(item: SettingItem) {
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

    fun onDismissDialogClicked() {
        updateState { it.copy(dialogData = null) }
    }

    fun onDialogActionClicked(action: DialogData.ButtonAction) {
        when (action) {
            DialogData.ButtonAction.Cancel -> {
                updateState { it.copy(dialogData = null) }
            }

            DialogData.ButtonAction.Ok -> {
                //TODO
                updateState { it.copy(dialogData = null) }
            }
        }
    }

}