package com.security.chat.multiplatform.features.profile.ui.screens.main

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.core.ui.entity.isLoading
import com.security.chat.multiplatform.features.profile.ui.screens.main.entity.DialogContent

@Immutable
internal data class ProfileMainState(
    val fetchInfoState: UiLceState,
    val login: String,
    val changeNameState: UiLceState,
    val loginChangeEnabled: Boolean,
    val dialogContent: DialogContent?,
) {
    val showLoading: Boolean = fetchInfoState.isLoading || changeNameState.isLoading
}
