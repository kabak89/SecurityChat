package com.security.chat.multiplatform.features.profile.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.profile.component.api.DeleteProfileComponent
import com.security.chat.multiplatform.features.profile.domain.DeleteProfileModel
import org.koin.core.qualifier.named

internal class DeleteProfileComponentImpl(
    componentContext: ComponentContext,
    private val onExit: () -> Unit,
    private val onProfileDeletion: () -> Unit,
) : DeleteProfileComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_DELETE_PROFILE,
    ) {

    init {
        doOnCreate {
            val deleteProfileModel: DeleteProfileModel = getKoin().get()
            deleteProfileModel.start(parentScope = getKoin().get(named(SCOPE_ID_DELETE_PROFILE)))
        }
    }

    override fun onBackClicked() {
        onExit()
    }

    override fun onProfileDeleted() {
        onProfileDeletion()
    }
}

public const val SCOPE_ID_DELETE_PROFILE: String = "SCOPE_ID_DELETE_PROFILE"
