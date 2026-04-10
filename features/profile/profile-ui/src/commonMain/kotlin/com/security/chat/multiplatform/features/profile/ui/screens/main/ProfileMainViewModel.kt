package com.security.chat.multiplatform.features.profile.ui.screens.main

import com.security.chat.multiplatform.common.core.ui.BaseViewModel

internal class ProfileMainViewModel : BaseViewModel<ProfileMainState, Unit>() {

    override fun createInitialState(): ProfileMainState {
        return ProfileMainState(
            title = "Profile",
        )
    }
}
