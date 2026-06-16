package com.security.chat.multiplatform.features.profile.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.profile.component.api.DeleteProfileComponent

internal class DeleteProfileComponentImpl(
    componentContext: ComponentContext,
    private val onExit: () -> Unit,
) : DeleteProfileComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_DELETE_PROFILE,
    ) {

    init {
        doOnCreate {
//            val profileModel: ProfileModel = getKoin().get()
//            profileModel.start(parentScope = getKoin().get(named(SCOPE_ID_DELETE_PROFILE)))
        }

//        val featureModules = listOf(
//            userNetworkManager,
//        )
//        getKoin().loadModules(featureModules)
//        doOnDestroy {
//            getKoin().unloadModules(featureModules)
//        }
    }

    override fun onBackClicked() {
        onExit()
    }
}

public const val SCOPE_ID_DELETE_PROFILE: String = "SCOPE_ID_DELETE_PROFILE"
