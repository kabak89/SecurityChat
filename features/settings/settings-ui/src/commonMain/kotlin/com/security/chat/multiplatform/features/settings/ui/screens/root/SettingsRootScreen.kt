package com.security.chat.multiplatform.features.settings.ui.screens.root

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.security.chat.multiplatform.features.settings.component.SettingsComponent
import com.security.chat.multiplatform.features.settings.ui.screens.main.SettingsMainScreen
import com.security.chat.multiplatform.features.settings.ui.screens.theme.ThemeScreen

@Composable
public fun SettingsRootScreen(
    component: SettingsComponent,
) {
    Children(
        stack = component.childStack,
        animation = predictiveBackAnimation(
            backHandler = component.backHandler,
            fallbackAnimation = stackAnimation(slide()),
            onBack = component::onBackClicked,
        ),
        content = {
            when (val child = it.instance) {
                is SettingsComponent.Child.SettingsMain -> {
                    SettingsMainScreen(
                        component = child.component,
                    )
                }

                is SettingsComponent.Child.Theme -> {
                    ThemeScreen(
                        component = child.component,
                    )
                }
            }
        },
    )
}