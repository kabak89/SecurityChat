package com.security.chat.multiplatform.features.root.ui.screens.root

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.authorize.ui.screens.authorize.AuthorizeScreen
import com.security.chat.multiplatform.features.main.ui.screens.main.MainScreen
import com.security.chat.multiplatform.features.onboarding.ui.screens.root.OnboardingRootScreen
import com.security.chat.multiplatform.features.root.component.api.RootComponent
import com.security.chat.multiplatform.features.settings.domain.entity.Theme
import com.security.chat.multiplatform.features.settings.domain.repo.SettingsRepo
import com.security.chat.multiplatform.features.splash.ui.screens.splash.SplashScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
public fun RootContent(
    rootComponent: RootComponent,
    isUpdateReady: Boolean = false,
    onUpdateRestart: () -> Unit = {},
    onUpdateDismiss: () -> Unit = {},
) {
    val settingsRepo = remember(rootComponent) { rootComponent.getKoin().get<SettingsRepo>() }
    val themeFlow = remember(settingsRepo) { settingsRepo.getTheme() }
    val theme = themeFlow.collectAsState(Theme.Auto).value

    RootScreenContent(
        theme = theme,
        isUpdateReady = isUpdateReady,
        onUpdateRestart = onUpdateRestart,
        onUpdateDismiss = onUpdateDismiss,
        content = { modifier ->
            Children(
                modifier = modifier,
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
        },
    )
}

@Composable
private fun RootScreenContent(
    theme: Theme,
    isUpdateReady: Boolean,
    onUpdateRestart: () -> Unit,
    onUpdateDismiss: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val useDarkTheme = when (theme) {
        Theme.Auto -> isSystemInDarkTheme()
        Theme.Dark -> true
        Theme.Light -> false
    }

    AppTheme(
        useDarkTheme = useDarkTheme,
    ) {
        val hazeState = rememberHazeState()
        Box(modifier = Modifier.fillMaxSize()) {
            content(Modifier.fillMaxSize().hazeSource(hazeState))
            if (isUpdateReady) {
                AppUpdateDialog(
                    hazeState = hazeState,
                    onRestart = onUpdateRestart,
                    onDismiss = onUpdateDismiss,
                )
            }
        }
    }
}

@Preview
@Composable
internal fun RootScreenPreview() {
    RootScreenContent(
        theme = Theme.Light,
        isUpdateReady = true,
        onUpdateRestart = {},
        onUpdateDismiss = {},
        content = { modifier ->
            Box(modifier = modifier.background(AppTheme.colors.backgroundPrimary))
        },
    )
}

@Preview
@Composable
internal fun RootScreenDarkPreview() {
    RootScreenContent(
        theme = Theme.Dark,
        isUpdateReady = true,
        onUpdateRestart = {},
        onUpdateDismiss = {},
        content = { modifier ->
            Box(modifier = modifier.background(AppTheme.colors.backgroundPrimary))
        },
    )
}
