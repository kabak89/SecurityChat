package com.security.chat.multiplatform.features.main.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.security.chat.multiplatform.features.add_chat.ui.AddChatScreen
import com.security.chat.multiplatform.features.chat.ui.screens.root.ChatRootScreen
import com.security.chat.multiplatform.features.chat_info.ui.screens.chatinfo.ChatInfoScreen
import com.security.chat.multiplatform.features.chats.ui.screens.chats.ChatsRootScreen
import com.security.chat.multiplatform.features.main.component.api.MainComponent
import com.security.chat.multiplatform.features.settings.ui.screens.root.SettingsRootScreen

@Composable
public fun MainScreen(
    component: MainComponent,
) {
    val stack by component.childStack.subscribeAsState()
    val activeChild = stack.active.instance

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        val isWide = maxWidth >= WIDE_SCREEN_MIN_WIDTH
        val showSidePanel = isWide && activeChild is MainComponent.Child.Chat

        val fallbackAnimation: StackAnimation<Any, Any> = if (isWide) {
            stackAnimation(fade())
        } else {
            stackAnimation(slide())
        }

        val animation = predictiveBackAnimation(
            backHandler = component.backHandler,
            fallbackAnimation = fallbackAnimation,
            onBack = component::onBackClicked,
        )

        Row(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            if (showSidePanel) {
                Box(
                    modifier = Modifier
                        .width(SIDE_PANEL_WIDTH),
                ) {
                    val chatsChild = stack.items
                        .find { it.instance is MainComponent.Child.Chats }
                        ?.instance as? MainComponent.Child.Chats

                    chatsChild?.let { ChatsRootScreen(component = it.component) }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f),
            ) {
                Children(
                    modifier = Modifier
                        .fillMaxSize(),
                    stack = component.childStack,
                    animation = animation,
                    content = {
                        when (val child = it.instance) {
                            is MainComponent.Child.Chats -> {
                                ChatsRootScreen(component = child.component)
                            }

                            is MainComponent.Child.Chat -> {
                                ChatRootScreen(component = child.component)
                            }

                            is MainComponent.Child.ChatInfo -> {
                                ChatInfoScreen(component = child.component)
                            }

                            is MainComponent.Child.Settings -> {
                                SettingsRootScreen(component = child.component)
                            }

                            is MainComponent.Child.AddChat -> {
                                AddChatScreen(component = child.component)
                            }
                        }
                    },
                )
            }
        }
    }
}

private val WIDE_SCREEN_MIN_WIDTH = 800.dp
private val SIDE_PANEL_WIDTH = 350.dp
