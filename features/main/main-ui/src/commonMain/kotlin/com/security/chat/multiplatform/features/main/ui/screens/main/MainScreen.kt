package com.security.chat.multiplatform.features.main.ui.screens.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.security.chat.multiplatform.features.chats.component.ChatsComponent
import com.security.chat.multiplatform.features.main.component.MainComponent

@Composable
public fun MainScreen(
    chatsScreen: @Composable (component: ChatsComponent) -> Unit,
    component: MainComponent,
) {
    Children(
        modifier = Modifier
            .fillMaxSize(),
        stack = component.childStack,
        animation = predictiveBackAnimation(
            backHandler = component.backHandler,
            fallbackAnimation = stackAnimation(slide()),
            onBack = component::onBackClicked,
        ),
        content = {
            when (val child = it.instance) {
                is MainComponent.Child.Chats -> {
                    chatsScreen(child.component)
                }

                is MainComponent.Child.Settings -> {
                    //TODO
                }
            }
        },
    )
}