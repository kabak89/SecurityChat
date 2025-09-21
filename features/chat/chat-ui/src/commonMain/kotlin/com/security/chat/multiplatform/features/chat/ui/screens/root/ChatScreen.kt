package com.security.chat.multiplatform.features.chat.ui.screens.root

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.security.chat.multiplatform.features.chat.component.ChatComponent
import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.PersonalChatScreen

@Composable
public fun ChatRootScreen(
    component: ChatComponent,
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
                is ChatComponent.Child.PersonalChat -> PersonalChatScreen(
                    component = child.component,
                )
            }
        },
    )
}