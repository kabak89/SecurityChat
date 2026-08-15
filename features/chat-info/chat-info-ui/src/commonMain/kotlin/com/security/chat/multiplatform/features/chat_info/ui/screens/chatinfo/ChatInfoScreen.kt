package com.security.chat.multiplatform.features.chat_info.ui.screens.chatinfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.components.ButtonContent
import com.security.chat.multiplatform.common.ui.kit.components.ButtonPrimary
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat_info.component.api.ChatInfoMainComponent
import org.jetbrains.compose.resources.stringResource
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.localization.generated.resources.chat_info_add_members

@Composable
public fun ChatInfoScreen(
    component: ChatInfoMainComponent,
) {
    Screen(
        component = component,
        screenName = "ChatInfoScreen",
    ) { state: ChatInfoState, vm: ChatInfoViewModel ->
        ChatInfoContent(
            state = state,
            onBackClicked = component::onBackClicked,
            onAddMembersClicked = component::onAddMembersClicked,
        )
    }
}

@Composable
private fun ChatInfoContent(
    state: ChatInfoState,
    onBackClicked: () -> Unit,
    onAddMembersClicked: () -> Unit,
) {
    Column(
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
        ButtonPrimary(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onClicked = onAddMembersClicked,
            content = ButtonContent.Text(
                text = stringResource(StringRes.chat_info_add_members),
            ),
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
            onAddMembersClicked = {},
        )
    }
}
