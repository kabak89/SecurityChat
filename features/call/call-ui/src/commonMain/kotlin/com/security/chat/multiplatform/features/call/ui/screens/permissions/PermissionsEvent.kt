package com.security.chat.multiplatform.features.call.ui.screens.permissions

internal sealed interface PermissionsEvent {

    data object RequestRecordAudioPermission : PermissionsEvent
}
