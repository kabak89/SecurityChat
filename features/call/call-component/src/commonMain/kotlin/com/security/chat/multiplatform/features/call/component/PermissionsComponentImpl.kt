package com.security.chat.multiplatform.features.call.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.call.component.api.PermissionsComponent

internal class PermissionsComponentImpl(
    private val onBack: () -> Unit,
    private val onGranted: () -> Unit,
    componentContext: ComponentContext,
) : PermissionsComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_CALL_PERMISSIONS,
    ) {

    override fun onBackClicked() {
        onBack()
    }

    override fun onPermissionsGranted() {
        onGranted()
    }
}

private const val SCOPE_ID_CALL_PERMISSIONS: String = "SCOPE_ID_CALL_PERMISSIONS"
