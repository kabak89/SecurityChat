package com.security.chat.multiplatform.features.chat_info.ui.screens.root

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.security.chat.multiplatform.features.chat_info.component.api.ChatInfoComponent
import com.security.chat.multiplatform.features.chat_info.ui.screens.addmember.AddMemberScreen
import com.security.chat.multiplatform.features.chat_info.ui.screens.chatinfo.ChatInfoScreen

@OptIn(ExperimentalDecomposeApi::class)
@Composable
public fun ChatInfoRootScreen(
    component: ChatInfoComponent,
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
                is ChatInfoComponent.Child.ChatInfoMain -> {
                    ChatInfoScreen(
                        component = child.component,
                    )
                }

                is ChatInfoComponent.Child.AddMember -> {
                    AddMemberScreen(
                        component = child.component,
                    )
                }
            }
        },
    )
}
