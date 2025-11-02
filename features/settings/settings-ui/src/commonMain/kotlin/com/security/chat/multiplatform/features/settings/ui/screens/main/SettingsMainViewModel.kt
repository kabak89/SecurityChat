package com.security.chat.multiplatform.features.settings.ui.screens.main

import com.security.chat.multiplatform.common.core.ui.BaseViewModel

internal class SettingsMainViewModel : BaseViewModel<SettingsMainState, SettingsMainEvent>() {

    override fun createInitialState(): SettingsMainState {
        return SettingsMainState()
    }

}