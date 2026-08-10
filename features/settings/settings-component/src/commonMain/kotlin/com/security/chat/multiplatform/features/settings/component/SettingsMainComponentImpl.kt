package com.security.chat.multiplatform.features.settings.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.settings.component.api.SettingsMainComponent
import com.security.chat.multiplatform.features.settings.domain.SettingsModel

internal class SettingsMainComponentImpl(
    private val onExit: () -> Unit,
    private val onGoToTheme: () -> Unit,
    private val onGoToProfile: () -> Unit,
    componentContext: ComponentContext,
) : SettingsMainComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_SETTINGS_MAIN,
    ) {

    init {
        doOnCreate {
            val settingsModel: SettingsModel = getKoin().get()
            settingsModel.start(parentScope = componentCoroutineScope)
        }
    }

    override fun onExitClicked() {
        onExit()
    }

    override fun onGoToThemeClicked() {
        onGoToTheme()
    }

    override fun onGoToProfileClicked() {
        onGoToProfile()
    }

}

public const val SCOPE_ID_SETTINGS_MAIN: String = "SCOPE_ID_SETTINGS_MAIN"