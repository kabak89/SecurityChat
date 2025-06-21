package com.security.chat.multiplatform.features.chats.ui.screens.chats

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.security.chat.multiplatform.features.chats.component.ChatsComponent
import com.security.chat.multiplatform.features.chats.ui.screens.addchat.AddChatScreen
import com.security.chat.multiplatform.features.chats.ui.screens.chatlist.ChatListScreen

@Composable
public fun ChatsScreen(
    component: ChatsComponent,
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
                is ChatsComponent.Child.ChatList -> ChatListScreen(component = child.component)
                is ChatsComponent.Child.AddChat -> AddChatScreen(component = child.component)
            }
        },
    )
}