package com.security.chat.multiplatform.features.onboarding.ui.screens.root

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.security.chat.multiplatform.features.onboarding.component.api.OnboardingComponent
import com.security.chat.multiplatform.features.onboarding.ui.screens.notificationpermission.NotificationPermissionScreen

@Composable
public fun OnboardingRootScreen(
    component: OnboardingComponent,
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
                is OnboardingComponent.Child.OnboardingMain -> {
                    NotificationPermissionScreen(
                        component = child.component,
                    )
                }
            }
        },
    )
}
