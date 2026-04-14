package com.security.chat.multiplatform.features.settings.ui.screens.main.entity

import androidx.compose.runtime.Immutable

@Immutable
internal data class DialogData(
    val title: String,
    val message: String,
    val positiveButtonTitle: String,
    val negativeButtonTitle: String,
    val positiveButtonAction: ButtonAction,
    val negativeButtonAction: ButtonAction,
) {

    @Immutable
    sealed interface ButtonAction {

        @Immutable
        data object Cancel : ButtonAction

        @Immutable
        data object Ok : ButtonAction
    }
}
