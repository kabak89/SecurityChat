package com.security.chat.multiplatform.features.settings.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.mesh.data.startMeshBluetooth
import com.security.chat.multiplatform.features.settings.component.api.NotificationsComponent

internal class NotificationsComponentImpl(
    private val onBack: () -> Unit,
    componentContext: ComponentContext,
) : NotificationsComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_SETTINGS_NOTIFICATIONS,
    ) {

    override fun onBackClicked() {
        onBack()
    }

    override fun onBluetoothAllowed() {
        startMeshBluetooth()
    }
}

public const val SCOPE_ID_SETTINGS_NOTIFICATIONS: String = "SCOPE_ID_SETTINGS_NOTIFICATIONS"
