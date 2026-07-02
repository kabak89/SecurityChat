package com.security.chat.multiplatform.features.chat.ui.screens.groupchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.component.api.GroupChatComponent

@Composable
internal fun GroupChatScreen(
    component: GroupChatComponent,
) {
    Screen(component) { state: GroupChatState, vm: GroupChatViewModel ->
        GroupChatContent(
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun GroupChatContent(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(AppTheme.colors.backgroundPrimary),
    )
}
