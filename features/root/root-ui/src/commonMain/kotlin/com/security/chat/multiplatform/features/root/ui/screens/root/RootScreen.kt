package com.security.chat.multiplatform.features.root.ui.screens.root

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.authorize.ui.screens.authorize.AuthorizeScreen
import com.security.chat.multiplatform.features.main.ui.screens.main.MainScreen
import com.security.chat.multiplatform.features.onboarding.ui.screens.root.OnboardingRootScreen
import com.security.chat.multiplatform.features.root.component.api.RootComponent
import com.security.chat.multiplatform.features.settings.data.storage.SettingsStorage
import com.security.chat.multiplatform.features.settings.data.storage.entity.ThemeSM
import com.security.chat.multiplatform.features.splash.ui.screens.splash.SplashScreen

@Composable
public fun RootContent(rootComponent: RootComponent) {
    val settingsStorage: SettingsStorage = rootComponent.getKoin().get()
    val theme = settingsStorage.getCurrentThemeFlow().collectAsState(ThemeSM.Auto).value

    val useDarkTheme = when (theme) {
        ThemeSM.Auto -> isSystemInDarkTheme()
        ThemeSM.Dark -> true
        ThemeSM.Light -> false
    }

    AppTheme(
        useDarkTheme = useDarkTheme,
    ) {
        Children(
            stack = rootComponent.childStack,
            animation = predictiveBackAnimation(
                backHandler = rootComponent.backHandler,
                fallbackAnimation = stackAnimation(slide()),
                onBack = rootComponent::onBackClicked,
            ),
            content = {
                when (val child = it.instance) {
                    is RootComponent.Child.Splash -> SplashScreen(component = child.component)
                    is RootComponent.Child.Authorize -> AuthorizeScreen(component = child.component)
                    is RootComponent.Child.Main -> MainScreen(component = child.component)
                    is RootComponent.Child.Onboarding -> OnboardingRootScreen(child.component)
                }
            },
        )
    }
}