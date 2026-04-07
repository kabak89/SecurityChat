package com.security.chat.multiplatform.features.settings.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.settings.component.api.ThemeComponent

internal class ThemeComponentImpl(
    private val onBack: () -> Unit,
    componentContext: ComponentContext,
) : ThemeComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_SETTINGS_THEME,
    ) {

    override fun onBackClicked() {
        onBack()
    }

}

public const val SCOPE_ID_SETTINGS_THEME: String = "SCOPE_ID_SETTINGS_THEME"