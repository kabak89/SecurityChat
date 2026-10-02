package com.security.chat.multiplatform.features.call.ui.screens.root

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.security.chat.multiplatform.features.call.component.api.CallComponent
import com.security.chat.multiplatform.features.call.ui.screens.call.CallScreen
import com.security.chat.multiplatform.features.call.ui.screens.permissions.PermissionsScreen

@OptIn(ExperimentalDecomposeApi::class)
@Composable
public fun CallRootScreen(
    component: CallComponent,
) {
    Children(
        stack = component.childStack,
        animation = predictiveBackAnimation(
            backHandler = component.backHandler,
            fallbackAnimation = stackAnimation(slide()),
            onBack = component::onBackClicked,
        ),
    ) {
        when (val child = it.instance) {
            is CallComponent.Child.Permissions -> PermissionsScreen(component = child.component)
            is CallComponent.Child.Main -> CallScreen(component = child.component)
        }
    }
}
