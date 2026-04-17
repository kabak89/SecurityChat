package com.security.chat.multiplatform.features.profile.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.profile.component.api.ProfileMainComponent
import com.security.chat.multiplatform.features.profile.domain.ProfileModel
import com.security.chat.multiplatform.features.user.data.network.di.userNetworkManager
import org.koin.core.qualifier.named

internal class ProfileMainComponentImpl(
    private val onExit: () -> Unit,
    componentContext: ComponentContext,
) : ProfileMainComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_PROFILE_MAIN,
    ) {

    init {
        doOnCreate {
            val profileModel: ProfileModel = getKoin().get()
            profileModel.start(parentScope = getKoin().get(named(SCOPE_ID_PROFILE_MAIN)))
        }

        val featureModules = listOf(
            userNetworkManager,
        )
        getKoin().loadModules(featureModules)
        doOnDestroy {
            getKoin().unloadModules(featureModules)
        }
    }

    override fun onBackClicked() {
        onExit()
    }
}

public const val SCOPE_ID_PROFILE_MAIN: String = "SCOPE_ID_PROFILE_MAIN"
