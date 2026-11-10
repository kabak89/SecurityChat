package com.security.chat.multiplatform.features.settings.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.profile.component.ProfileComponentImpl
import com.security.chat.multiplatform.features.settings.component.api.SettingsComponent
import com.security.chat.multiplatform.features.settings.component.api.SettingsComponent.Child.Logs
import com.security.chat.multiplatform.features.settings.component.api.SettingsComponent.Child.Notifications
import com.security.chat.multiplatform.features.settings.component.api.SettingsComponent.Child.Profile
import com.security.chat.multiplatform.features.settings.component.api.SettingsComponent.Child.SettingsMain
import com.security.chat.multiplatform.features.settings.component.api.SettingsComponent.Child.Theme
import com.security.chat.multiplatform.features.settings.domain.di.settingsDomainModule
import com.security.chat.multiplatform.features.settings.ui.di.settingsUiModule
import kotlinx.serialization.Serializable

public class SettingsComponentImpl(
    private val onExit: () -> Unit,
    componentContext: ComponentContext,
) : SettingsComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_SETTINGS,
    ) {

    private val navigation = StackNavigation<Params>()

    init {
        val featureModules = listOf(
            settingsUiModule,
            settingsDomainModule,
        )
        getKoin().loadModules(featureModules)
        doOnDestroy {
            getKoin().unloadModules(featureModules)
        }
    }

    override val childStack: Value<ChildStack<*, SettingsComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Params.serializer(),
            initialConfiguration = Params.SettingsMain,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override fun onBackClicked() {
        navigation.pop()
    }

    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): SettingsComponent.Child {
        return when (params) {
            Params.SettingsMain -> {
                SettingsMain(
                    component = SettingsMainComponentImpl(
                        componentContext = componentContext,
                        onExit = onExit,
                        onGoToTheme = { navigation.push(Params.Theme) },
                        onGoToNotifications = { navigation.push(Params.Notifications) },
                        onGoToProfile = { navigation.push(Params.Profile) },
                        onGoToLogs = { navigation.pushNew(Params.Logs) },
                    ),
                )
            }

            Params.Theme -> {
                Theme(
                    component = ThemeComponentImpl(
                        componentContext = componentContext,
                        onBack = navigation::pop,
                    ),
                )
            }

            Params.Profile -> {
                Profile(
                    ProfileComponentImpl(
                        componentContext = componentContext,
                        onExit = navigation::pop,
                    ),
                )
            }

            Params.Notifications -> {
                Notifications(
                    component = NotificationsComponentImpl(
                        componentContext = componentContext,
                        onBack = navigation::pop,
                    ),
                )
            }

            Params.Logs -> Logs
        }
    }

    @Serializable
    private sealed class Params {
        @Serializable
        data object SettingsMain : Params()

        @Serializable
        data object Theme : Params()

        @Serializable
        data object Notifications : Params()

        @Serializable
        data object Profile : Params()

        @Serializable
        data object Logs : Params()
    }
}

public const val SCOPE_ID_SETTINGS: String = "SCOPE_ID_SETTINGS"
