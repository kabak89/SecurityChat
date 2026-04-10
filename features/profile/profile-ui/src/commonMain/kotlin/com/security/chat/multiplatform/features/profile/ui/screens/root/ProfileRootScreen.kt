package com.security.chat.multiplatform.features.profile.ui.screens.root

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.security.chat.multiplatform.features.profile.component.api.ProfileComponent
import com.security.chat.multiplatform.features.profile.ui.screens.main.ProfileMainScreen

@OptIn(ExperimentalDecomposeApi::class)
@Composable
public fun ProfileRootScreen(
    component: ProfileComponent,
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
                is ProfileComponent.Child.ProfileMain -> {
                    ProfileMainScreen(
                        component = child.component,
                    )
                }
            }
        },
    )
}
