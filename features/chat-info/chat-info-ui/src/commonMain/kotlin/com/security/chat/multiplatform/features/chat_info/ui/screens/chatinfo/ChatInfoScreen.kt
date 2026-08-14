package com.security.chat.multiplatform.features.chat_info.ui.screens.chatinfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat_info.component.api.ChatInfoComponent
import securitychat.common.icons_kit.generated.resources.ic_back

@Composable
public fun ChatInfoScreen(
    component: ChatInfoComponent,
) {
    Screen(
        component = component,
        screenName = "ChatInfoScreen",
    ) { state: ChatInfoState, vm: ChatInfoViewModel ->
        ChatInfoContent(
            state = state,
            onBackClicked = component::onBackClicked,
        )
    }
}

@Composable
private fun ChatInfoContent(
    state: ChatInfoState,
    onBackClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundPrimary),
    ) {
        ToolbarComponent(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            startContent = SideContent.Button(
                icon = DrawableRes.ic_back,
                onClicked = onBackClicked,
            ),
            centerContent = CenterContent.Title(text = state.title),
        )

        Text(
            modifier = Modifier.align(Alignment.Center),
            text = "Chat Information Screen",
            style = AppTheme.typography.body,
            color = AppTheme.colors.textPrimary,
        )
    }
}

@Preview
@Composable
internal fun ChatInfoScreenPreview() {
    AppTheme {
        ChatInfoContent(
            state = ChatInfoState(
                title = "Chat Info: 12345",
            ),
            onBackClicked = {},
        )
    }
}
