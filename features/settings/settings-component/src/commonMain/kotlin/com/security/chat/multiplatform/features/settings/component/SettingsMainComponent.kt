package com.security.chat.multiplatform.features.settings.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface SettingsMainComponent : BaseComponent, DiScopeHolder {

    public fun onExitClicked()
    public fun onUserLogOuted()
    public fun onGoToThemeClicked()

}

public class SettingsMainComponentImpl(
    private val onExit: () -> Unit,
    private val onLogout: () -> Unit,
    private val onGoToTheme: () -> Unit,
    componentContext: ComponentContext,
) : SettingsMainComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_SETTINGS_MAIN,
    ) {

    override fun onExitClicked() {
        onExit()
    }

    override fun onUserLogOuted() {
        onLogout()
    }

    override fun onGoToThemeClicked() {
        onGoToTheme()
    }

}

public const val SCOPE_ID_SETTINGS_MAIN: String = "SCOPE_ID_SETTINGS_MAIN"