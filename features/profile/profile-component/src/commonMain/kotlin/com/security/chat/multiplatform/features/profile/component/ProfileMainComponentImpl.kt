package com.security.chat.multiplatform.features.profile.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.profile.component.api.ProfileMainComponent
import com.security.chat.multiplatform.features.profile.domain.ProfileModel

internal class ProfileMainComponentImpl(
    private val onExit: () -> Unit,
    private val onDeleteProfile: () -> Unit,
    componentContext: ComponentContext,
) : ProfileMainComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_PROFILE_MAIN,
    ) {

    init {
        doOnCreate {
            val profileModel: ProfileModel = getKoin().get()
            profileModel.start(parentScope = componentCoroutineScope)
        }
    }

    override fun onBackClicked() {
        onExit()
    }

    override fun onDeleteProfileClicked() {
        onDeleteProfile()
    }
}

public const val SCOPE_ID_PROFILE_MAIN: String = "SCOPE_ID_PROFILE_MAIN"
